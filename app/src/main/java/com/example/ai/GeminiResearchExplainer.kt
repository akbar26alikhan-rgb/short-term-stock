package com.example.ai

import com.example.BuildConfig
import com.example.model.TradeSetup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiResearchExplainer(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    /**
     * Returns an AI-powered swing trade rationale and catalyst analysis.
     * If Gemini API key is missing or request fails, returns structured rule-based technical summary.
     */
    suspend fun explainTradeSetup(
        setup: TradeSetup,
        userCustomApiKey: String? = null
    ): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        val apiKey = when {
            !userCustomApiKey.isNullOrBlank() -> userCustomApiKey
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (apiKey.isBlank()) {
            return@withContext Pair(
                "Offline Rule-Based Research Summary:\n" +
                        "• Setup: ${setup.setupType.label} on ${setup.symbol} (Score: ${setup.score.compositeScore}/100)\n" +
                        "• Technical Catalyst: Price ₹${String.format("%.2f", setup.closePrice)} with RVOL ${String.format("%.2f", setup.indicators.rvol)}x. RSI at ${String.format("%.1f", setup.indicators.rsi)}.\n" +
                        "• Key Levels: Planned Entry ₹${String.format("%.2f", setup.entryPrice)}, Stop-Loss ₹${String.format("%.2f", setup.stopLoss)}, Target 1 ₹${String.format("%.2f", setup.target1)} (R:R ${String.format("%.2f", setup.riskRewardRatio)}).\n" +
                        "• Relative Strength: Outperforming Nifty 50 by ${String.format("%+.2f", setup.indicators.rsNifty50_20d)}% over 20 trading sessions.\n\n" +
                        "(Note: Gemini API key not configured. Enable in Settings or AI Studio Secrets for automated neural research explanations.)",
                false
            )
        }

        try {
            val prompt = """
                You are a senior quantitative swing trading research analyst for the Indian Stock Market (NSE).
                Analyze the following setup:
                Stock: ${setup.symbol} (${setup.companyName}, Sector: ${setup.sector})
                Setup Type: ${setup.setupType.label}
                Current Close: ₹${setup.closePrice}
                Planned Entry: ₹${setup.entryPrice}
                Stop Loss: ₹${setup.stopLoss}
                Target 1: ₹${setup.target1} (R:R ${String.format("%.2f", setup.riskRewardRatio)})
                RSI (14): ${String.format("%.1f", setup.indicators.rsi)}
                RVOL: ${String.format("%.2f", setup.indicators.rvol)}x
                20 EMA: ₹${String.format("%.2f", setup.indicators.ema20)}, 50 EMA: ₹${String.format("%.2f", setup.indicators.ema50)}
                20-day Support: ₹${String.format("%.2f", setup.indicators.support20)}, Resistance: ₹${String.format("%.2f", setup.indicators.resistance20)}
                Relative Strength vs Nifty 50: ${String.format("%+.2f", setup.indicators.rsNifty50_20d)}%

                Provide a concise, professional swing trading research note in 3 crisp sections:
                1. Structural Setup Rationale (price action & volume dynamics)
                2. Risk Factors & Invalidation Trigger (market conditions that nullify this thesis)
                3. Execution Strategy (entry timing and partial profit locking approach)
                Keep it grounded in risk management, concise (under 180 words), and end with a reminder that this is educational research, not brokerage advice.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        }
                        put("parts", parts)
                    })
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("maxOutputTokens", 600)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            // Using official recommended model: gemini-3.5-flash
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Pair(
                    "Gemini API returned error HTTP ${response.code}. Falling back to rule-based analysis:\n\n${setup.rationale}",
                    false
                )
            }

            val respBody = response.body?.string() ?: return@withContext Pair(setup.rationale, false)
            val respJson = JSONObject(respBody)
            val candidates = respJson.optJSONArray("candidates")
            val text = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")

            if (!text.isNullOrBlank()) {
                Pair(text.trim(), true)
            } else {
                Pair(setup.rationale, false)
            }
        } catch (e: Exception) {
            Pair(
                "Gemini request failed: ${e.message ?: "Network error"}. Rule-based analysis:\n\n${setup.rationale}",
                false
            )
        }
    }

    /**
     * Summarizes a news article or cluster of company headlines
     */
    suspend fun summarizeNews(
        headlines: List<String>,
        userCustomApiKey: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = when {
            !userCustomApiKey.isNullOrBlank() -> userCustomApiKey
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        if (apiKey.isBlank()) {
            return@withContext "AI summary unavailable without API Key. Top headline: ${headlines.firstOrNull() ?: "No headlines"}"
        }

        try {
            val prompt = "Summarize the key market takeaways for Indian equity traders from these headlines in 2 bullet points:\n" +
                    headlines.joinToString("\n") { "• $it" }

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        }
                        put("parts", parts)
                    })
                }
                put("contents", contents)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(mediaType))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val respBody = response.body?.string() ?: return@withContext "Empty response"
                val respJson = JSONObject(respBody)
                val text = respJson.optJSONArray("candidates")?.optJSONObject(0)
                    ?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)
                    ?.optString("text")
                text ?: "Summary unavailable"
            } else {
                "Unable to generate summary (HTTP ${response.code})"
            }
        } catch (e: Exception) {
            "Summary generation failed: ${e.message}"
        }
    }
}
