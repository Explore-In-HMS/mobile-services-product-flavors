// Copyright 2020. Explore in HMS. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at

// http://www.apache.org/licenses/LICENSE-2.0

// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.hms.lib.mobileservicesproductflavors.push

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.hms.lib.mobileservicesproductflavors.push.model.Provider
import com.hms.lib.mobileservicesproductflavors.push.model.Token

class PushServiceImpl(context: Context): PushService(context) {
    companion object {
        private const val TAG = "GooglePushServiceImpl"
    }

    private val firebaseMessaging = FirebaseMessaging.getInstance()
    override fun autoInitEnabled(enable: Boolean) {
        firebaseMessaging.isAutoInitEnabled = enable
    }

    override fun getToken(): Work<Token> {
        val work: Work<Token> = Work()
        firebaseMessaging.token
            .addOnSuccessListener { work.onSuccess(Token(Provider.Google, it)) }
            .addOnFailureListener { work.onFailure(it) }
            .addOnCanceledListener { work.onCanceled() }
        return work
    }

    override fun subscribeToTopic(topic: String): Work<Unit> {
        val work: Work<Unit> = Work()
        firebaseMessaging.subscribeToTopic(topic)
            .addOnSuccessListener { work.onSuccess(Unit) }
            .addOnFailureListener { work.onFailure(it) }
            .addOnCanceledListener { work.onCanceled() }

        return work
    }

    override fun unsubscribeFromTopic(topic: String): Work<Unit> {
        val work: Work<Unit> = Work()
        firebaseMessaging.unsubscribeFromTopic(topic)
            .addOnSuccessListener { work.onSuccess(Unit) }
            .addOnFailureListener { work.onFailure(it) }
            .addOnCanceledListener { work.onCanceled() }
        return work
    }
}