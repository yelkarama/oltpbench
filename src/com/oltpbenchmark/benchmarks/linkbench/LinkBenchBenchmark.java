/******************************************************************************
 *  Copyright 2015 by OLTPBenchmark Project                                   *
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

package com.oltpbenchmark.benchmarks.linkbench;

import com.oltpbenchmark.WorkloadConfiguration;
import com.oltpbenchmark.api.BenchmarkModule;
import com.oltpbenchmark.api.Loader;
import com.oltpbenchmark.api.Worker;
import com.oltpbenchmark.benchmarks.linkbench.procedures.AddNode;
import com.oltpbenchmark.benchmarks.linkbench.utils.ConfigUtil;
import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;


public class LinkBenchBenchmark extends BenchmarkModule {

    private static final Logger LOG = Logger.getLogger(LinkBenchBenchmark.class);
    private final Properties props;

    public LinkBenchBenchmark(WorkloadConfiguration workConf) throws Exception {
        super("linkbench", workConf, true);
        LinkBenchConfiguration linkBenchConf = new LinkBenchConfiguration(workConf);
        props = new Properties();
        props.load(new FileInputStream(linkBenchConf.getConfigFile()));
    }

    @Override
    protected List<Worker<? extends BenchmarkModule>> makeWorkersImpl(boolean verbose) throws IOException {
        List<Worker<? extends BenchmarkModule>> workers = new ArrayList<Worker<? extends BenchmarkModule>>();
        Random masterRandom = createMasterRNG(props);
        for (int i = 0; i < workConf.getTerminals(); ++i) {
            workers.add(new LinkBenchWorker(this, i, new Random(masterRandom.nextLong()), props, workConf.getTerminals()));
        } // FOR
        return workers;
    }

    @Override
    protected Loader<LinkBenchBenchmark> makeLoaderImpl() throws SQLException {
        return new LinkBenchLoader(this);
    }

    @Override
    protected Package getProcedurePackageImpl() {
        return AddNode.class.getPackage();
    }

    /**
     * Create a new random number generated, optionally seeded to a known
     * value from the config file.  If seed value not provided, a seed
     * is chosen.  In either case the seed is logged for later reproducibility.
     * @param props
     * @return
     */
    private Random createMasterRNG(Properties props) {
        long seed;
        if (props.containsKey(LinkBenchConstants.REQUEST_RANDOM_SEED)) {
            seed = ConfigUtil.getLong(props, LinkBenchConstants.REQUEST_RANDOM_SEED);
            LOG.info("Using configured random seed " + LinkBenchConstants.REQUEST_RANDOM_SEED + "=" + seed);
        } else {
            seed = System.nanoTime() ^ (long) LinkBenchConstants.REQUEST_RANDOM_SEED.hashCode();
            LOG.info("Using random seed " + seed + " since " + LinkBenchConstants.REQUEST_RANDOM_SEED
                    + " not specified");
        }

        SecureRandom masterRandom;
        try {
            masterRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            LOG.warn("SHA1PRNG not available, defaulting to default SecureRandom" +
            " implementation");
            masterRandom = new SecureRandom();
        }
        masterRandom.setSeed(ByteBuffer.allocate(8).putLong(seed).array());

        // Can be used to check that rng is behaving as expected
        LOG.info("First number generated by master " + LinkBenchConstants.REQUEST_RANDOM_SEED +
                ": " + masterRandom.nextLong());
        return masterRandom;
    }
}
