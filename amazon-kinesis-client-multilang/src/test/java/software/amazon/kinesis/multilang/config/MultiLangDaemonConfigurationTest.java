/*
 * Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.kinesis.multilang.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;
import software.amazon.kinesis.retrieval.fanout.FanOutConfig;
import software.amazon.kinesis.retrieval.polling.PollingConfig;

@RunWith(MockitoJUnitRunner.class)
public class MultiLangDaemonConfigurationTest {

    private BeanUtilsBean utilsBean;
    private ConvertUtilsBean convertUtilsBean;

    @Mock
    private ShardRecordProcessorFactory shardRecordProcessorFactory;

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        convertUtilsBean = new ConvertUtilsBean();
        utilsBean = new BeanUtilsBean(convertUtilsBean);
    }

    public MultiLangDaemonConfiguration baseConfiguration() {
        MultiLangDaemonConfiguration configuration = new MultiLangDaemonConfiguration(utilsBean, convertUtilsBean);
        configuration.setApplicationName("Test");
        configuration.setStreamName("Test");
        configuration.getKinesisCredentialsProvider().set("class", DefaultCredentialsProvider.class.getName());

        return configuration;
    }

    @Test
    public void testSetPrimitiveValue() {
        MultiLangDaemonConfiguration configuration = baseConfiguration();
        configuration.setMaxLeasesForWorker(10);

        MultiLangDaemonConfiguration.ResolvedConfiguration resolvedConfiguration = configuration
                .resolvedConfiguration(shardRecordProcessorFactory);

        assertThat(resolvedConfiguration.leaseManagementConfig.maxLeasesForWorker(), equalTo(10));
    }

    @Test
    public void testDefaultRetrievalConfig() {
        MultiLangDaemonConfiguration configuration = baseConfiguration();

        MultiLangDaemonConfiguration.ResolvedConfiguration resolvedConfiguration = configuration
                .resolvedConfiguration(shardRecordProcessorFactory);

        assertThat(resolvedConfiguration.getRetrievalConfig().retrievalSpecificConfig(),
                instanceOf(FanOutConfig.class));
    }

    @Test
    public void testDefaultRetrievalConfigWithPollingConfigSet() {
        MultiLangDaemonConfiguration configuration = baseConfiguration();
        configuration.setMaxRecords(10);

        MultiLangDaemonConfiguration.ResolvedConfiguration resolvedConfiguration = configuration
                .resolvedConfiguration(shardRecordProcessorFactory);

        assertThat(resolvedConfiguration.getRetrievalConfig().retrievalSpecificConfig(),
                instanceOf(PollingConfig.class));
    }

    @Test
    public void testFanoutRetrievalMode() {
        MultiLangDaemonConfiguration configuration = baseConfiguration();
        configuration.setRetrievalMode(RetrievalMode.FANOUT);

        MultiLangDaemonConfiguration.ResolvedConfiguration resolvedConfiguration = configuration
                .resolvedConfiguration(shardRecordProcessorFactory);

        assertThat(resolvedConfiguration.getRetrievalConfig().retrievalSpecificConfig(),
                instanceOf(FanOutConfig.class));
    }

    @Test
    public void testPollingRetrievalMode() {
        MultiLangDaemonConfiguration configuration = baseConfiguration();
        configuration.setRetrievalMode(RetrievalMode.POLLING);

        MultiLangDaemonConfiguration.ResolvedConfiguration resolvedConfiguration = configuration
                .resolvedConfiguration(shardRecordProcessorFactory);

        assertThat(resolvedConfiguration.getRetrievalConfig().retrievalSpecificConfig(),
                instanceOf(PollingConfig.class));
    }

    @Test
    public void testRetrievalModeSetForPollingString() throws Exception {
        MultiLangDaemonConfiguration configuration = baseConfiguration();

        utilsBean.setProperty(configuration, "retrievalMode", RetrievalMode.POLLING.name().toLowerCase());

        MultiLangDaemonConfiguration.ResolvedConfiguration resolvedConfiguration = configuration
                .resolvedConfiguration(shardRecordProcessorFactory);

        assertThat(resolvedConfiguration.getRetrievalConfig().retrievalSpecificConfig(),
                instanceOf(PollingConfig.class));
    }

    @Test
    public void testRetrievalModeSetForFanoutString() throws Exception {
        MultiLangDaemonConfiguration configuration = baseConfiguration();

        utilsBean.setProperty(configuration, "retrievalMode", RetrievalMode.FANOUT.name().toLowerCase());

        MultiLangDaemonConfiguration.ResolvedConfiguration resolvedConfiguration = configuration
                .resolvedConfiguration(shardRecordProcessorFactory);

        assertThat(resolvedConfiguration.getRetrievalConfig().retrievalSpecificConfig(),
                instanceOf(FanOutConfig.class));
    }

    @Test
    public void testInvalidRetrievalMode() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unknown retrieval type");

        MultiLangDaemonConfiguration configuration = baseConfiguration();

        utilsBean.setProperty(configuration, "retrievalMode", "invalid");
    }

    @Test
    public void testFanoutConfigSetConsumerName() {
        String consumerArn = "test-consumer";

        MultiLangDaemonConfiguration configuration = baseConfiguration();

        configuration.setRetrievalMode(RetrievalMode.FANOUT);
        configuration.getFanoutConfig().setConsumerArn(consumerArn);

        MultiLangDaemonConfiguration.ResolvedConfiguration resolvedConfiguration = configuration
                .resolvedConfiguration(shardRecordProcessorFactory);

        assertThat(resolvedConfiguration.getRetrievalConfig().retrievalSpecificConfig(),
                instanceOf(FanOutConfig.class));
        FanOutConfig fanOutConfig = (FanOutConfig) resolvedConfiguration.getRetrievalConfig().retrievalSpecificConfig();

        assertThat(fanOutConfig.consumerArn(), equalTo(consumerArn));
    }

}