package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiAssistantRepo"

class AiAssistantRepository {

    private val roadmapSchema = Schema.obj(
        properties = mapOf(
            "summary" to Schema.string(description = "A concise, encouraging 1-2 sentence roadmap overview"),
            "milestones" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "title" to Schema.string(description = "Short milestone title"),
                        "description" to Schema.string(description = "Actionable explanation of what to master"),
                        "duration" to Schema.string(description = "Estimated timeframe like Week 1"),
                        "exercise" to Schema.string(description = "Hands-on practice drill for this milestone")
                    )
                ),
                description = "4 to 5 sequential beginner-friendly learning milestones"
            )
        )
    )

    private val exerciseSchema = Schema.obj(
        properties = mapOf(
            "exerciseTitle" to Schema.string(description = "Title of the practice drill"),
            "instructions" to Schema.string(description = "Step-by-step exercise instructions"),
            "successCriteria" to Schema.string(description = "How the learner knows they mastered the drill"),
            "estimatedMinutes" to Schema.integer(description = "Minutes needed to complete")
        )
    )

    private val roadmapModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = roadmapSchema
            }
        )
    }

    private val exerciseModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = exerciseSchema
            }
        )
    }

    private val insightModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    suspend fun generateRoadmap(
        skillName: String,
        targetLevel: String,
        userTeachSkills: List<String>
    ): Result<LearningRoadmap> {
        return try {
            val prompt = buildString {
                append("Create a practical peer-learning roadmap for '$skillName' at '$targetLevel' level. ")
                if (userTeachSkills.isNotEmpty()) {
                    append("The learner already knows ${userTeachSkills.joinToString(", ")}, so bridge concepts cleanly where helpful. ")
                }
                append("Include 4 sequential milestones with realistic timeframes and a concrete hands-on practice exercise for each.")
            }

            val response = withTimeout(60_000L) {
                roadmapModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("AI returned an empty roadmap.")
            val root = JSONObject(jsonText)
            val summary = root.optString("summary", "Structured roadmap for $skillName").take(600)
            val milestonesJson = root.getJSONArray("milestones")
            val milestones = (0 until milestonesJson.length().coerceAtMost(8)).map { i ->
                val obj = milestonesJson.getJSONObject(i)
                RoadmapMilestone(
                    title = obj.optString("title", "Milestone ${i + 1}").take(120),
                    description = obj.optString("description", "").take(300),
                    duration = obj.optString("duration", "Week ${i + 1}").take(40),
                    exercise = obj.optString("exercise", "").take(300),
                    completed = false
                )
            }
            require(milestones.isNotEmpty()) { "Roadmap had zero milestones." }
            Result.success(
                LearningRoadmap(
                    skillName = skillName.trim().take(80),
                    targetLevel = targetLevel.trim().take(40),
                    summary = summary,
                    milestones = milestones,
                    completedCount = 0L,
                    totalCount = milestones.size.toLong()
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Roadmap generation timed out", e)
            Result.failure(Exception("Roadmap generation timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Roadmap generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to generate learning roadmap."))
        }
    }

    suspend fun generatePracticeExercise(
        skillName: String,
        milestoneTitle: String
    ): Result<String> {
        return try {
            val prompt = "Design a fresh 15-to-25 minute hands-on practice exercise for '$skillName' focused on '$milestoneTitle'."
            val response = withTimeout(45_000L) {
                exerciseModel.generateContent(prompt)
            }
            val jsonText = response.text ?: throw IllegalStateException("AI returned an empty exercise.")
            val obj = JSONObject(jsonText)
            val title = obj.optString("exerciseTitle", milestoneTitle)
            val instructions = obj.optString("instructions", "")
            val criteria = obj.optString("successCriteria", "")
            val mins = obj.optInt("estimatedMinutes", 20)
            val formatted = "$title (~$mins min)\n\n$instructions\n\nGoal: $criteria"
            Result.success(formatted)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Exercise generation timed out", e)
            Result.failure(Exception("Practice drill request timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Exercise generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to generate practice exercise."))
        }
    }

    suspend fun generateMatchInsight(
        userTeachSkills: List<String>,
        userLearnSkills: List<String>,
        partner: MentorPartner,
        matchScore: Int
    ): Result<String> {
        return try {
            val prompt = """
                You are SkillSwap's peer matching coach. In 3 concise bullet points (no markdown headers), explain why this $matchScore% skill swap works and suggest a 45-minute first swap agenda.
                Learner teaches: ${userTeachSkills.joinToString(", ").ifBlank { "General Tech" }}
                Learner wants: ${userLearnSkills.joinToString(", ").ifBlank { partner.teachSkill }}
                Partner (${partner.name}) teaches: ${partner.teachSkill} (${partner.teachLevel})
                Partner wants: ${partner.wantSkill}
            """.trimIndent()

            val response = withTimeout(45_000L) {
                insightModel.generateContent(prompt)
            }
            val text = response.text?.trim()
            if (text.isNullOrBlank()) {
                Result.failure(Exception("AI returned an empty match insight."))
            } else {
                Result.success(text)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Match insight timed out", e)
            Result.failure(Exception("Match insight timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Match insight failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to generate match insight."))
        }
    }
}
