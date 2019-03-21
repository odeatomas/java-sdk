/****************************************************************************
 * Copyright 2019, Optimizely, Inc. and contributors                        *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ***************************************************************************/

package com.optimizely.ab.notification;

import com.optimizely.ab.notification.decisionInfo.DecisionNotification;

import javax.annotation.Nonnull;

public abstract class DecisionNotificationListener implements NotificationListener, DecisionNotificationListenerInterface {

    /**
     * Base notify called with var args.  This method parses the parameters and calls the abstract method.
     *
     * @param args - variable argument list based on the type of notification.
     */
    @Override
    public final void notify(Object... args) {
        assert (args[0] instanceof DecisionNotification);
        DecisionNotification decisionNotification = (DecisionNotification) args[0];
        onDecision(decisionNotification);

    }

    @Override
    public abstract void onDecision(@Nonnull DecisionNotification decisionNotification);
}
