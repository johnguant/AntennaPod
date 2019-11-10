package de.danoeh.antennapod.viewmodel

import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.ViewModel
import de.danoeh.antennapod.core.storage.FeedRepository
import de.danoeh.antennapod.fragment.SubscriptionItemMoveListener

class SubscriptionViewModel : ViewModel(), SubscriptionItemMoveListener {
    val subscriptions = LiveDataReactiveStreams.fromPublisher(FeedRepository().getSubscriptions())
    override fun onItemMove(source: Int, target: Int) {
        FeedRepository().moveSubscription(source, target)
    }
}