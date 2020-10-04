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

import java.util.concurrent.ExecutorService;

/**
 * @author Gael Lalire
 */
public class ExecutorServiceReaperHelper implements ReaperHelper {

    private ExecutorService executorService;

    public ExecutorServiceReaperHelper(final ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void reap() {
        executorService.shutdownNow();
    }

}
