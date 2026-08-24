import com.everycue.feature.track.*
import com.everycue.feature.renew.*

fun main() {
    val today = 20_000L
    val track = TrackItem("1", "Milk", TrackCategory.GROCERY, 1.0, "pack", null, today, "Fridge", "", 7, 0, 0)
    check(track.expiryState(today) == ExpiryState.EXPIRES_TODAY)
    check(track.copy(expiryEpochDay = today + 7).expiryState(today) == ExpiryState.EXPIRING_SOON)
    check(track.copy(expiryEpochDay = today + 8).expiryState(today) == ExpiryState.FRESH)
    check(track.copy(expiryEpochDay = today - 1).expiryState(today) == ExpiryState.EXPIRED)

    val renewal = RenewalItem("1", "Passport", RenewalType.DOCUMENT, today, 30, "", "", "", null, 0, 0)
    check(renewal.dueState(today) == DueState.DUE_TODAY)
    check(renewal.copy(dueEpochDay = today + 30).dueState(today) == DueState.DUE_SOON)
    check(renewal.copy(dueEpochDay = today + 31).dueState(today) == DueState.UPCOMING)
    check(renewal.copy(dueEpochDay = today - 1).dueState(today) == DueState.OVERDUE)

    println("DOMAIN_CHECKS_OK")
}

