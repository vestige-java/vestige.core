/*
 * Copyright 2020 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.gaellalire.vestige.core.weak;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * @author Gael Lalire
 */
public class ReapableWeakReference extends WeakReference<Object> {

    private ReapableWeakReference[] last;

    private ReaperHelper reaperHelper;

    private ReapableWeakReference previous;

    private ReapableWeakReference next;

    public ReapableWeakReference(final ReapableWeakReference[] last, final Object referent, final ReferenceQueue<Object> referenceQueue, final ReaperHelper reaperHelper) {
        super(referent, referenceQueue);
        this.last = last;
        this.reaperHelper = reaperHelper;
        this.previous = last[0];
        if (previous != null) {
            previous.next = this;
        }
    }

    public ReapableWeakReference getPrevious() {
        return previous;
    }

    @Override
    public void clear() {
        if (next == null) {
            last[0] = previous;
            if (previous != null) {
                previous.next = null;
            }
        } else {
            next.previous = previous;
        }
        if (previous != null) {
            previous.next = next;
        }
        reaperHelper.reap();
        super.clear();
    }

}
