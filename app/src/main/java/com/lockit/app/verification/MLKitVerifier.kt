package com.lockit.app.verification

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Free, fully on-device image verification using ML Kit's built-in Image
 * Labeling model. No API key, no network call, no per-use cost.
 *
 * This is intentionally "dumb" compared to an LLM: it returns generic labels
 * like "Food", "Furniture", "Plant" with confidence scores, and we just check
 * for keyword overlap against what the user (or our label-suggester) expects.
 */
object MLKitVerifier {

    data class VerificationResult(
        val passed: Boolean,
        val detectedLabels: List<String>,
        val matchedLabel: String?
    )

    suspend fun verify(bitmap: Bitmap, expectedLabels: List<String>): VerificationResult {
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)

        val labels = suspendCancellableCoroutine<List<String>> { cont ->
            labeler.process(image)
                .addOnSuccessListener { result ->
                    cont.resume(result.map { it.text.lowercase() })
                }
                .addOnFailureListener {
                    cont.resume(emptyList())
                }
        }

        if (expectedLabels.isEmpty()) {
            // No specific expectation set (manual/loose task) - any detected
            // content at all counts as "a photo was genuinely taken".
            return VerificationResult(passed = labels.isNotEmpty(), detectedLabels = labels, matchedLabel = null)
        }

        val expectedLower = expectedLabels.map { it.lowercase().trim() }
        val match = labels.firstOrNull { detected ->
            expectedLower.any { expected -> detected.contains(expected) || expected.contains(detected) }
        }

        return VerificationResult(
            passed = match != null,
            detectedLabels = labels,
            matchedLabel = match
        )
    }
}
