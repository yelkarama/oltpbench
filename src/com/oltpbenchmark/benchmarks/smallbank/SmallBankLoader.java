package com.oltpbenchmark.benchmarks.smallbank;

import com.oltpbenchmark.api.Loader;
import com.oltpbenchmark.catalog.Table;
import com.oltpbenchmark.util.RandomDistribution.DiscreteRNG;
import com.oltpbenchmark.util.RandomDistribution.Gaussian;
import com.oltpbenchmark.util.SQLUtil;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * SmallBankBenchmark Loader
 * @author pavlo
 */
public class SmallBankLoader extends Loader<SmallBankBenchmark> {
    private static final Logger LOG = Logger.getLogger(SmallBankLoader.class);

    private final String sqlAccts;
    private final String sqlSavings;
    private final String sqlChecking;

    private final long numAccounts;
    private final int custNameLength;

    public SmallBankLoader(SmallBankBenchmark benchmark) {
        super(benchmark);

        Table catalogAccts = this.benchmark.getTableCatalog(SmallBankConstants.TABLENAME_ACCOUNTS);
        assert(catalogAccts != null);
        Table catalogSavings = this.benchmark.getTableCatalog(SmallBankConstants.TABLENAME_SAVINGS);
        assert(catalogSavings != null);
        Table catalogChecking = this.benchmark.getTableCatalog(SmallBankConstants.TABLENAME_CHECKING);
        assert(catalogChecking != null);

        this.sqlAccts = SQLUtil.getInsertSQL(catalogAccts, this.getDatabaseType());
        this.sqlSavings = SQLUtil.getInsertSQL(catalogSavings, this.getDatabaseType());
        this.sqlChecking = SQLUtil.getInsertSQL(catalogChecking, this.getDatabaseType());

        this.numAccounts = benchmark.numAccounts;
        this.custNameLength = SmallBankBenchmark.getCustomerNameLength(catalogAccts);
    }

    @Override
    public List<LoaderThread> createLoaderThreads() throws SQLException {
        List<LoaderThread> threads = new ArrayList<LoaderThread>();
        int batchSize = 100000;
        long start = 0;
        while (start < this.numAccounts) {
            long stop = Math.min(start + batchSize, this.numAccounts);
            threads.add(new Generator(start, stop));
            start = stop;
        }
        return (threads);
    }

    /**
     * Thread that can generate a range of accounts
     */
    private class Generator extends LoaderThread {
        private final long start;
        private final long stop;
        private final DiscreteRNG randBalance;

        PreparedStatement stmtAccts;
        PreparedStatement stmtSavings;
        PreparedStatement stmtChecking;

        public Generator(long start, long stop) throws SQLException {
            super();
            this.start = start;
            this.stop = stop;
            this.randBalance = new Gaussian(SmallBankLoader.this.benchmark.rng(),
                                            SmallBankConstants.MIN_BALANCE,
                                            SmallBankConstants.MAX_BALANCE);
        }

        @Override
        public void load(Connection conn) throws SQLException {
            try {
                this.stmtAccts = conn.prepareStatement(SmallBankLoader.this.sqlAccts);
                this.stmtSavings = conn.prepareStatement(SmallBankLoader.this.sqlSavings);
                this.stmtChecking = conn.prepareStatement(SmallBankLoader.this.sqlChecking);

                final String acctNameFormat = "%0"+custNameLength+"d";
                int batchSize = 0;
                for (long acctId = this.start; acctId < this.stop; acctId++) {
                    // ACCOUNT
                    String acctName = String.format(acctNameFormat, acctId);
                    stmtAccts.setLong(1, acctId);
                    stmtAccts.setString(2, acctName);
                    stmtAccts.addBatch();

                    // CHECKINGS
                    stmtChecking.setLong(1, acctId);
                    stmtChecking.setInt(2, this.randBalance.nextInt());
                    stmtChecking.addBatch();

                    // SAVINGS
                    stmtSavings.setLong(1, acctId);
                    stmtSavings.setInt(2, this.randBalance.nextInt());
                    stmtSavings.addBatch();

                    if (++batchSize >= SmallBankConstants.BATCH_SIZE) {
                        this.loadTables(conn);
                        batchSize = 0;
                    }

                } // FOR
                if (batchSize > 0) {
                    this.loadTables(conn);
                }
            } catch (SQLException ex) {
                LOG.error("Failed to load data", ex);
                throw new RuntimeException(ex);
            }
        }

        private void loadTables(Connection conn) throws SQLException {
            this.stmtAccts.executeBatch();
            this.stmtSavings.executeBatch();
            this.stmtChecking.executeBatch();
            conn.commit();

        }
    }

}
