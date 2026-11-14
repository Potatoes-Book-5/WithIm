package com.potatoes.book5.realestate.domain.error

sealed interface DomainError {
    data object Offline : DomainError
    data object Timeout : DomainError
    data object MissingApiKey : DomainError
    data object InvalidResponse : DomainError
    data object ProviderRejected : DomainError
    data class Http(val statusCode: Int) : DomainError
    data class Unsupported(val capability: String) : DomainError
    /** 입력이 규칙을 만족하지 않음. 예: 관심 조건을 하나도 고르지 않음(F-004). */
    data class Invalid(val reason: String) : DomainError
    data class Unknown(val cause: Throwable) : DomainError
}

sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: DomainError) : Outcome<Nothing>
}
