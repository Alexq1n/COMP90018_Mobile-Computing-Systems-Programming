package com.group5.roammate.pet

import java.util.Locale
import kotlin.math.roundToInt

enum class PetDialogueLanguage(val tag: String) {
    English("en-AU"), Chinese("zh-CN");

    fun text(english: String, chinese: String): String = if (this == Chinese) chinese else english
}

enum class PetDialogueTopic { Weather, Clothes, Trip, Care, Greeting, Help }

data class PetDialogueReply(val text: String, val topic: PetDialogueTopic)

/**
 * A local, deterministic travel companion. It uses the same weather/outfit/itinerary as the UI.
 * No remote language model, secret key, invented places, or promise of a scheduled notification.
 * Speech recognition is deliberately a separate adapter: typing works on every device.
 */
object PetDialogueEngine {
    fun reply(
        input: String,
        state: PetUiState,
        tripContext: PetTripContext,
        isMoving: Boolean,
        language: PetDialogueLanguage,
        previousTopic: PetDialogueTopic? = null,
    ): PetDialogueReply {
        val text = input.lowercase(Locale.ROOT).trim()
        fun has(english: String, vararg chinese: String): Boolean =
            Regex("\\b(?:$english)\\b").containsMatchIn(text) || chinese.any(text::contains)

        val isFollowUp = has("why|how come|tell me more|more", "为什么", "为啥", "详细", "再说说")
        val topic = when {
            has("tired|exhausted|thirsty|hungry|rest|break|care", "累", "渴", "饿", "休息", "关心") -> PetDialogueTopic.Care
            has("wear|wearing|clothes|clothing|outfit|jacket|fashion", "穿", "衣服", "衣着", "时装", "换装") -> PetDialogueTopic.Clothes
            has("trip|itinerary|next|plan|recommend|suggest|remind|reminder|visit|go", "行程", "下一站", "去哪", "推荐", "建议", "提醒", "接下来") -> PetDialogueTopic.Trip
            has("weather|rain|raining|sun|sunny|wind|temperature|forecast|snow|hot|cold", "天气", "下雨", "雨伞", "晴", "风", "温度", "预报", "下雪", "热", "冷") -> PetDialogueTopic.Weather
            isFollowUp && previousTopic != null -> previousTopic
            has("hello|hi|hey|thanks|thank you", "你好", "嗨", "谢谢") -> PetDialogueTopic.Greeting
            else -> PetDialogueTopic.Help
        }
        val answer = when (topic) {
            PetDialogueTopic.Weather -> {
                val future = has("tomorrow|week|weekend", "明天", "后天", "周末", "下周")
                val preface = if (future) language.text(
                    "I don't have a forecast for that future date. ", "我还没有那个未来日期的天气预报。",
                ) else ""
                preface + weatherSummary(state.weather, language) + " " + weatherCare(state.weather, language)
            }
            PetDialogueTopic.Clothes -> clothingSummary(state, language)
            PetDialogueTopic.Trip -> {
                val scheduling = has("set|schedule|alarm|notify|remind|reminder", "设置", "定时", "到点", "通知", "提醒", "闹钟")
                val preface = if (scheduling) language.text(
                    "I can talk through your plan here, but I haven't set a notification. ",
                    "我可以在这里帮你梳理行程，但没有设置定时通知。",
                ) else ""
                preface + tripSummary(tripContext, language) + " " + weatherCare(state.weather, language)
            }
            PetDialogueTopic.Care -> {
                val movement = if (isMoving) language.text("We've been moving. ", "我们正在走动。") else ""
                movement + language.text(
                    "Let's take a comfortable break and have some water. We can continue at your pace. ",
                    "找个舒服的地方歇一会儿，喝点水吧。我们可以按你的节奏继续。",
                ) + weatherCare(state.weather, language)
            }
            PetDialogueTopic.Greeting -> language.text(
                "Hi, I'm Buddy! I'm here with you. Ask about the weather, my outfit, or our next stop.",
                "你好，我是 Buddy！我会陪着你。可以问我天气、穿搭，或者下一站去哪。",
            )
            PetDialogueTopic.Help -> language.text(
                "I can help with the weather, our next stop, my outfit, and taking a break. Try ‘What's the weather?’ or ‘Where do we go next?’",
                "我可以介绍天气、下一站和穿搭，也会提醒你休息。试试问“今天天气怎么样”或“下一站去哪”。",
            )
        }
        return PetDialogueReply(answer.trim(), topic)
    }

    private fun weatherSummary(weather: PetWeatherSnapshot, language: PetDialogueLanguage): String {
        if (!weather.isCurrent) return unavailableWeather(language)
        val condition = when (weather.condition) {
            PetWeatherCondition.Clear -> language.text("clear", "晴朗")
            PetWeatherCondition.Cloudy -> language.text("cloudy", "多云")
            PetWeatherCondition.Fog -> language.text("foggy", "有雾")
            PetWeatherCondition.Rain -> language.text("rainy", "下雨")
            PetWeatherCondition.Storm -> language.text("stormy", "雷雨")
            PetWeatherCondition.Snow -> language.text("snowy", "下雪")
            PetWeatherCondition.Unknown -> return unavailableWeather(language)
        }
        val temperature = weather.temperatureC.roundToInt()
        val wind = weather.windSpeedKmh.roundToInt()
        val current = language.text(
            "The latest weather for ${weather.locationLabel} is $condition, $temperature degrees Celsius, with wind around $wind kilometres an hour.",
            "${weather.locationLabel}最新天气是$condition，气温${temperature}摄氏度，风速约每小时${wind}公里。",
        )
        val low = weather.todayLowC
        val high = weather.todayHighC
        val rainChance = weather.todayRainChancePercent
        return if (weather.forecastDate != null && low != null && high != null) {
            current + language.text(
                " The forecast for ${weather.forecastDate} is ${low.roundToInt()} to ${high.roundToInt()} degrees" +
                    (rainChance?.let { ", with a $it percent chance of precipitation." } ?: "."),
                "${weather.forecastDate}的预报气温为${low.roundToInt()}到${high.roundToInt()}摄氏度" +
                    (rainChance?.let { "，降水概率为百分之$it。" } ?: "。"),
            )
        } else {
            current + language.text(" I only have current conditions, not an all-day forecast.", "目前只有当前天气，不能代表全天预报。")
        }
    }

    private fun clothingSummary(state: PetUiState, language: PetDialogueLanguage): String {
        fun outfitName(outfit: PetOutfit): String = if (language == PetDialogueLanguage.English) outfit.label else when (outfit) {
            PetOutfit.Everyday -> "日常装"
            PetOutfit.Sunshine -> "遮阳装"
            PetOutfit.Raincoat -> "雨衣"
            PetOutfit.Windbreaker -> "防风外套"
            PetOutfit.Winter -> "保暖装"
            PetOutfit.Explorer -> "探险装"
            PetOutfit.Streetwear -> "街头卫衣"
            PetOutfit.Festival -> "庆典夹克"
            PetOutfit.Pajamas -> "星星睡衣"
        }
        val actual = outfitName(state.outfit)
        val chosen = state.wardrobeChoice.manualOutfit
        val explanation = if (chosen != null && chosen != state.outfit) language.text(
            "I'm wearing $actual for the weather. Your ${outfitName(chosen)} is saved and returns when conditions are mild. ",
            "我现在穿着${actual}应对天气。你选的${outfitName(chosen)}已保留，天气温和时会自动换回来。",
        ) else language.text("I'm wearing $actual. ", "我现在穿着$actual。")
        return explanation + if (state.weather.isCurrent) weatherCare(state.weather, language) else unavailableWeather(language)
    }

    private fun tripSummary(trip: PetTripContext, language: PetDialogueLanguage): String {
        val nextStop = trip.nextStopName?.takeIf(String::isNotBlank)
            ?: return language.text("There isn't a next stop in the plan yet. Add one in Trip and I can remind you here.", "行程里还没有下一站。先在 Trip 里添加，我就能在这里提醒你。")
        val time = trip.nextStopTime?.takeIf(String::isNotBlank)
        return if (trip.isDemo) language.text(
            "The sample itinerary shows $nextStop${time?.let { " at $it" } ?: ""}. This is demo data, not your saved booking.",
            "示例行程的下一站是$nextStop${time?.let { "，时间是$it" } ?: ""}。这是演示数据，不是你的真实预订。",
        ) else language.text(
            "Your next planned stop is $nextStop${time?.let { " at $it" } ?: ""}.",
            "你的下一站是$nextStop${time?.let { "，计划时间是$it" } ?: ""}。",
        )
    }

    private fun unavailableWeather(language: PetDialogueLanguage): String = language.text(
        "I don't have fresh weather for the selected place yet. Check the weather connection before relying on outdoor advice.",
        "我还没有所选地点的最新天气。安排户外活动前，先确认天气更新成功。",
    )

    private fun weatherCare(weather: PetWeatherSnapshot, language: PetDialogueLanguage): String {
        if (!weather.isCurrent) return ""
        return when {
            weather.condition == PetWeatherCondition.Storm -> language.text("Let's stay indoors until the storm passes; we can review the next stop together.", "先待在室内，等雷雨过去，再一起看看下一站的安排。")
            weather.condition == PetWeatherCondition.Snow || weather.temperatureC <= 9 -> language.text("Keep warm and watch for slippery paths. A sheltered stop could be more comfortable.", "注意保暖，也留意路面是否湿滑。有遮挡的休息点会更舒服。")
            weather.condition == PetWeatherCondition.Rain -> language.text("Bring rain protection. Consider an indoor stop and allow extra travel time.", "带好雨具吧。可以考虑室内活动，并多留一些路上时间。")
            weather.windSpeedKmh >= 25 -> language.text("Zip up a windproof layer and secure your hat. A sheltered route may be nicer.", "穿好防风外套，拿稳帽子。走有遮挡的路线会更舒服。")
            weather.condition == PetWeatherCondition.Fog -> language.text("Visibility may be reduced. Take your time and keep to clearly marked paths.", "有雾时视线可能受影响，慢慢走，沿着标识清楚的路前进。")
            weather.temperatureC >= 25 && weather.isDay -> language.text("Bring water and look for shade. A short break between stops sounds good.", "带上水，多找阴凉处。两站之间歇一会儿也很好。")
            weather.temperatureC >= 25 -> language.text("It's still warm after dark. Bring water and take a break somewhere cool between stops.", "入夜后仍然有些热，带好水，两站之间找个凉快的地方歇一会儿。")
            else -> language.text("Let's keep a comfortable pace and leave time for a little break.", "按舒服的节奏走吧，也给自己留一点休息时间。")
        }
    }
}
