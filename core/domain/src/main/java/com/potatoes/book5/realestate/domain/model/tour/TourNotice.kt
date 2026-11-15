package com.potatoes.book5.realestate.domain.model.tour
import java.time.Instant

data class TourNotice(val id:String,val source:String,val type:String,val title:String,val url:String,
                      val publishedAt:Instant?,val address:String?=null,val scope:String="NATIONAL",val publishedDate:java.time.LocalDate?=null)
data class StoredTourNotice(val notice:TourNotice,val version:String,val observedAt:Instant,val readVersion:String?)
data class TourSourceState(val source:String,val state:String,val attemptedAt:Instant)
