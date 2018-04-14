/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hive.testutils.dtest;

import org.apache.hive.testutils.dtest.impl.SimpleAnalyzerFactory;
import org.apache.hive.testutils.dtest.impl.Utils;

import java.io.IOException;

public abstract class ResultAnalyzerFactory {
  public static final String PROPERTY = "dtest.result.analyzer.factory";

  static ResultAnalyzerFactory get() throws IOException {
    String factoryClassName = System.getProperty(PROPERTY);
    if (factoryClassName == null) factoryClassName = SimpleAnalyzerFactory.class.getName();

    Class<? extends ResultAnalyzerFactory> clazz = Utils.getClass(factoryClassName,
        ResultAnalyzerFactory.class);
    return Utils.newInstance(clazz);
  }

  public abstract ResultAnalyzer getAnalyzer();


}
