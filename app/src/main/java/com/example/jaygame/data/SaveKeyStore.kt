package com.example.jaygame.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import org.json.JSONArray
import org.json.JSONObject

object SaveKeyStore {
    private const val KEY_ALIAS = "jaygame_save_hmac"
    private const val TAG = "SaveKeyStore"
    // 소프트웨어 fallback 키 (KeyStore 사용 불가 기기용)
    private val FALLBACK_KEY = "jaygame_sw_hmac_2026".toByteArray(Charsets.UTF_8)

    private fun getOrCreateKey(): SecretKey {
        return try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (ks.containsAlias(KEY_ALIAS)) {
                (ks.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
            } else {
                val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore")
                keyGen.init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN).build())
                keyGen.generateKey()
            }
        } catch (e: Exception) {
            Log.w(TAG, "AndroidKeyStore unavailable, using software fallback", e)
            SecretKeySpec(FALLBACK_KEY, "HmacSHA256")
        }
    }

    fun hmacSha256(data: String): String {
        return try {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(getOrCreateKey())
            mac.doFinal(data.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "HMAC computation failed", e)
            // 최후 fallback: FNV-1a 스타일 해시
            data.hashCode().toLong().toString(16)
        }
    }

    /** JSON 키를 알파벳 순서로 정렬하여 canonical form 생성 (키 순서 비결정성 해결) */
    fun canonicalize(json: JSONObject): String {
        val sorted = JSONObject()
        json.keys().asSequence().sorted().forEach { key ->
            val value = json.get(key)
            sorted.put(key, when (value) {
                is JSONObject -> JSONObject(canonicalize(value))
                is JSONArray -> canonicalizeArray(value)
                else -> value
            })
        }
        return sorted.toString()
    }

    private fun canonicalizeArray(arr: JSONArray): JSONArray {
        val result = JSONArray()
        for (i in 0 until arr.length()) {
            val value = arr.get(i)
            result.put(when (value) {
                is JSONObject -> JSONObject(canonicalize(value))
                is JSONArray -> canonicalizeArray(value)
                else -> value
            })
        }
        return result
    }
}
