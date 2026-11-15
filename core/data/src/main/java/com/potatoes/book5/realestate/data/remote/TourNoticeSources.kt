package com.potatoes.book5.realestate.data.remote

import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.tour.TourNotice
import com.potatoes.book5.realestate.domain.port.TourNoticeSource
import java.io.StringReader
import java.net.URI
import java.time.*
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.util.concurrent.TimeUnit

/** Title/link/date only. No article body, user location or private request context. */
object TourNoticeParsers {
    fun rss(raw:String): List<TourNotice> {
        require(raw.length<=500_000 && !raw.contains("<!DOCTYPE",true) && !raw.contains("<!ENTITY",true))
        val factory=DocumentBuilderFactory.newInstance().apply { isExpandEntityReferences=false;isNamespaceAware=false }
        val builder=factory.newDocumentBuilder().apply { setEntityResolver { _,_ -> InputSource(StringReader("")) } }
        val document=builder.parse(InputSource(StringReader(raw)))
        require(document.documentElement.tagName=="rss")
        val rows=document.getElementsByTagName("item")
        require(rows.length<=200)
        return (0 until rows.length).mapNotNull { i ->
            val item=rows.item(i) as Element
            fun text(name:String)=item.getElementsByTagName(name).item(0)?.textContent?.trim().orEmpty()
            val title=text("title")
            if(listOf("주택","부동산","청약","분양","임대","공급").none { it in title }) return@mapNotNull null
            require(title.isNotBlank() && title.length<=500)
            val link=URI(text("link"));require(link.scheme in listOf("http","https") && link.host in listOf("www.molit.go.kr","molit.go.kr") && link.userInfo==null)
            val url=URI("https",null,link.host,link.port,link.path,link.query,null).toASCIIString()
            val published=runCatching { ZonedDateTime.parse(text("pubDate"),DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }.getOrNull()
            TourNotice("notice:molit-news:$url","NEWS","NATIONAL_PRESS",title,url,published,scope="NATIONAL")
        }.distinctBy { it.id }
    }
    data class ApplyPage(val total:Int,val rows:List<TourNotice>)
    fun applyhome(raw:String): ApplyPage {
        require(raw.length<=1_000_000)
        val root=JSONObject(raw);val data=root.getJSONArray("data")
        val total=root.getInt("matchCount");require(total in 0..500 && data.length()<=100)
        val rows=(0 until data.length()).map { i ->
            val row=data.getJSONObject(i)
            fun value(name:String)=row.get(name).takeUnless { it==JSONObject.NULL }?.toString()?.trim().orEmpty()
            val manage=value("HOUSE_MANAGE_NO");val notice=value("PBLANC_NO");val name=value("HOUSE_NM");val address=value("HSSPLY_ADRES")
            require(manage.isNotBlank() && notice.isNotBlank() && name.isNotBlank() && name.length<=500 && address.isNotBlank())
            val date=LocalDate.parse(value("RCRIT_PBLANC_DE"))
            TourNotice("notice:applyhome:$manage:$notice","SUBSCRIPTION","SUBSCRIPTION",name,
                "https://www.data.go.kr/data/15098547/openapi.do",null,address,"DISTRICT_ADDRESS_MATCH",date)
        }
        return ApplyPage(total,rows)
    }
}

class MolitRssNoticeSource(private val fetch: suspend ()->Outcome<String> = { getNoticeUrl("https://www.molit.go.kr/dev/board/board_rss.jsp?rss_id=NEWS") }) : TourNoticeSource {
    override val id="NEWS"
    override val configured=true
    override suspend fun fetch(): Outcome<List<TourNotice>> = when(val r=fetch.invoke()) {
        is Outcome.Failure -> r
        is Outcome.Success -> try { Outcome.Success(TourNoticeParsers.rss(r.value)) }
            catch(e:CancellationException) { throw e } catch(_:Exception) { Outcome.Failure(DomainError.InvalidResponse) }
    }
}

/** Explicit provider configuration. Never silently reuse a MOLIT key on another service. */
class ApplyhomeNoticeSource(private val serviceKey:String?=null,private val clock:Clock=Clock.systemUTC(),
    private val fetchPage: suspend (Int,String,String)->Outcome<String> = { page,since,key ->
        val url=okhttp3.HttpUrl.Builder().scheme("https").host("api.odcloud.kr").addPathSegments("api/ApplyhomeInfoDetailSvc/v1/getAPTLttotPblancDetail")
            .addQueryParameter("serviceKey",key).addQueryParameter("page",page.toString()).addQueryParameter("perPage","100")
            .addQueryParameter("returnType","JSON").addQueryParameter("cond[RCRIT_PBLANC_DE::GTE]",since).build()
        getNoticeUrl(url.toString())
    }) : TourNoticeSource {
    override val id="SUBSCRIPTION"
    override val configured get()=!serviceKey.isNullOrBlank()
    override suspend fun fetch(): Outcome<List<TourNotice>> {
        if(!configured) return Outcome.Failure(DomainError.MissingApiKey)
        val since=clock.instant().atZone(ZoneId.of("Asia/Seoul")).toLocalDate().minusDays(7).toString()
        val rows=mutableListOf<TourNotice>();var total:Int?=null
        for(page in 1..5) {
            val raw=when(val r=fetchPage(page,since,serviceKey!!.trim())) { is Outcome.Failure -> return r; is Outcome.Success -> r.value }
            val result=try { TourNoticeParsers.applyhome(raw) } catch(_:Exception) { return Outcome.Failure(DomainError.InvalidResponse) }
            if(total!=null && total!=result.total) return Outcome.Failure(DomainError.InvalidResponse)
            total=result.total;rows+=result.rows
            if(rows.size==total) return if(rows.distinctBy { it.id }.size==rows.size) Outcome.Success(rows) else Outcome.Failure(DomainError.InvalidResponse)
            if(result.rows.isEmpty() || rows.size>total) return Outcome.Failure(DomainError.InvalidResponse)
        }
        return Outcome.Failure(DomainError.InvalidResponse)
    }
}
private val noticeHttp by lazy { OkHttpClient.Builder().connectTimeout(5,TimeUnit.SECONDS).readTimeout(10,TimeUnit.SECONDS)
    .callTimeout(15,TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build() }
private suspend fun getNoticeUrl(url:String): Outcome<String> = withContext(Dispatchers.IO) {
    try {
        noticeHttp.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if(!response.isSuccessful) Outcome.Failure(DomainError.Http(response.code))
            else {
                val body=response.body ?: return@use Outcome.Failure(DomainError.InvalidResponse)
                val source=body.source();source.request(1_000_001)
                if(source.buffer.size>1_000_000) Outcome.Failure(DomainError.InvalidResponse) else Outcome.Success(body.string())
            }
        }
    } catch(e:CancellationException) { throw e }
    catch(_:java.io.InterruptedIOException) { Outcome.Failure(DomainError.Timeout) }
    catch(_:java.io.IOException) { Outcome.Failure(DomainError.Offline) }
    catch(_:Exception) { Outcome.Failure(DomainError.InvalidResponse) }
}
