package com.github.saiprasadkrishnamurthy.datawatcher.rest

import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyNotFoundException
import com.github.saiprasadkrishnamurthy.datawatcher.model.PolicyTamperedException
import com.github.saiprasadkrishnamurthy.datawatcher.model.SignatureInvalidException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class RestResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(value = [PolicyNotFoundException::class])
    protected fun handleNotFound(ex: RuntimeException, request: WebRequest): ResponseEntity<Any> {
        val bodyOfResponse = "Policy not found!"
        return handleExceptionInternal(ex, bodyOfResponse, HttpHeaders(), HttpStatus.NOT_FOUND, request)
    }

    @ExceptionHandler(value = [PolicyTamperedException::class])
    protected fun handlePolicyTampered(ex: RuntimeException, request: WebRequest): ResponseEntity<Any> {
        val bodyOfResponse = "Policy tampered!"
        return handleExceptionInternal(ex, bodyOfResponse, HttpHeaders(), HttpStatus.CONFLICT, request)
    }

    @ExceptionHandler(value = [SignatureInvalidException::class])
    protected fun handleSignatureFailures(ex: RuntimeException, request: WebRequest): ResponseEntity<Any> {
        val bodyOfResponse = "Signature of the party supplying the data is invalid!"
        return handleExceptionInternal(ex, bodyOfResponse, HttpHeaders(), HttpStatus.UNAUTHORIZED, request)
    }
}