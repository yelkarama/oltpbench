/******************************************************************************
 *  Copyright 2016 by OLTPBenchmark Project                                   *
 *                                                                            *
 *  Licensed under the Apache License, Version 2.0 (the "License");           *
 *  you may not use this file except in compliance with the License.          *
 *  You may obtain a copy of the License at                                   *
 *                                                                            *
 *    http://www.apache.org/licenses/LICENSE-2.0                              *
 *                                                                            *
 *  Unless required by applicable law or agreed to in writing, software       *
 *  distributed under the License is distributed on an "AS IS" BASIS,         *
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  *
 *  See the License for the specific language governing permissions and       *
 *  limitations under the License.                                            *
 ******************************************************************************/


package com.oltpbenchmark.benchmarks.smallbank;

import com.oltpbenchmark.api.AbstractTestBenchmarkModule;

public class TestSmallBankBenchmark extends AbstractTestBenchmarkModule<SmallBankBenchmark> {

    public static final Class<?> PROC_CLASSES[] = {
        Amalgamate.class,
        Balance.class,
        DepositChecking.class,
        SendPayment.class,
        TransactSavings.class,
        WriteCheck.class,
    };

	@Override
	protected void setUp() throws Exception {
		super.setUp(SmallBankBenchmark.class, PROC_CLASSES);
	}

}
