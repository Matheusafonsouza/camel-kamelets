/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.kamelets.utils.transform.aws.ddb;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.component.aws2.ddb.Ddb2Constants;
import org.apache.camel.component.aws2.ddb.Ddb2Operations;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeAction;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.AttributeValueUpdate;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;

class JsonToDdbModelConverterTest {

    private DefaultCamelContext camelContext;

    private final ObjectMapper mapper = new ObjectMapper();

    private final JsonToDdbModelConverter processor = new JsonToDdbModelConverter();

    private final String keyJson = "{" +
            "\"name\": \"Rajesh Koothrappali\"" +
            "}";

    private final String itemJson = "{" +
            "\"name\": \"Rajesh Koothrappali\"," +
            "\"age\": 29," +
            "\"super-heroes\": [\"batman\", \"spiderman\", \"wonderwoman\"]," +
            "\"issues\": [5, 3, 9, 1]," +
            "\"girlfriend\": null," +
            "\"doctorate\": true" +
            "}";

    @BeforeEach
    void setup() {
        this.camelContext = new DefaultCamelContext();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldMapPutItemHeaders() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);

        exchange.getMessage().setBody(mapper.readTree(itemJson));

        processor.process(Ddb2Operations.PutItem.name(), exchange);

        Assertions.assertTrue(exchange.getMessage().hasHeaders());
        Assertions.assertEquals(Ddb2Operations.PutItem, exchange.getMessage().getHeader(Ddb2Constants.OPERATION));
        Assertions.assertEquals(ReturnValue.ALL_OLD.toString(), exchange.getMessage().getHeader(Ddb2Constants.RETURN_VALUES));

        assertAttributeValueMap(exchange.getMessage().getHeader(Ddb2Constants.ITEM, Map.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldMapUpdateItemHeaders() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);

        exchange.getMessage().setBody(mapper.readTree("{\"key\": " + keyJson + ", \"item\": " + itemJson + "}"));

        processor.process(Ddb2Operations.UpdateItem.name(), exchange);

        Assertions.assertTrue(exchange.getMessage().hasHeaders());
        Assertions.assertEquals(Ddb2Operations.UpdateItem, exchange.getMessage().getHeader(Ddb2Constants.OPERATION));
        Assertions.assertEquals(ReturnValue.ALL_NEW.toString(), exchange.getMessage().getHeader(Ddb2Constants.RETURN_VALUES));

        Map<String, AttributeValue> attributeValueMap = exchange.getMessage().getHeader(Ddb2Constants.KEY, Map.class);
        Assertions.assertEquals(1L, attributeValueMap.size());
        Assertions.assertEquals(AttributeValue.builder().s("Rajesh Koothrappali").build(), attributeValueMap.get("name"));

        assertAttributeValueUpdateMap(exchange.getMessage().getHeader(Ddb2Constants.UPDATE_VALUES, Map.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldMapDeleteItemHeaders() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);

        exchange.getMessage().setBody(mapper.readTree("{\"key\": " + keyJson + "}"));

        processor.process(Ddb2Operations.DeleteItem.name(), exchange);

        Assertions.assertTrue(exchange.getMessage().hasHeaders());
        Assertions.assertEquals(Ddb2Operations.DeleteItem, exchange.getMessage().getHeader(Ddb2Constants.OPERATION));
        Assertions.assertEquals(ReturnValue.ALL_OLD.toString(), exchange.getMessage().getHeader(Ddb2Constants.RETURN_VALUES));

        Map<String, AttributeValue> attributeValueMap = exchange.getMessage().getHeader(Ddb2Constants.KEY, Map.class);
        Assertions.assertEquals(1L, attributeValueMap.size());
        Assertions.assertEquals(AttributeValue.builder().s("Rajesh Koothrappali").build(), attributeValueMap.get("name"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldMapNestedObjects() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);

        exchange.getMessage().setBody(mapper.readTree("{\"user\":" + itemJson + "}"));

        processor.process(Ddb2Operations.PutItem.name(), exchange);

        Assertions.assertTrue(exchange.getMessage().hasHeaders());
        Assertions.assertEquals(Ddb2Operations.PutItem, exchange.getMessage().getHeader(Ddb2Constants.OPERATION));
        Assertions.assertEquals(ReturnValue.ALL_OLD.toString(), exchange.getMessage().getHeader(Ddb2Constants.RETURN_VALUES));

        Map<String, AttributeValue> attributeValueMap = exchange.getMessage().getHeader(Ddb2Constants.ITEM, Map.class);
        Assertions.assertEquals(1L, attributeValueMap.size());

        Assertions.assertEquals("AttributeValue(M={name=AttributeValue(S=Rajesh Koothrappali), " +
                "age=AttributeValue(N=29), " +
                "super-heroes=AttributeValue(SS=[batman, spiderman, wonderwoman]), " +
                "issues=AttributeValue(NS=[5, 3, 9, 1]), " +
                "girlfriend=AttributeValue(NUL=true), " +
                "doctorate=AttributeValue(BOOL=true)})", attributeValueMap.get("user").toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldMapEmptyJson() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);

        exchange.getMessage().setBody(mapper.readTree("{}"));

        processor.process(Ddb2Operations.PutItem.name(), exchange);

        Assertions.assertTrue(exchange.getMessage().hasHeaders());
        Assertions.assertEquals(Ddb2Operations.PutItem, exchange.getMessage().getHeader(Ddb2Constants.OPERATION));
        Assertions.assertEquals(ReturnValue.ALL_OLD.toString(), exchange.getMessage().getHeader(Ddb2Constants.RETURN_VALUES));

        Map<String, AttributeValue> attributeValueMap = exchange.getMessage().getHeader(Ddb2Constants.ITEM, Map.class);
        Assertions.assertEquals(0L, attributeValueMap.size());
    }

    @Test()
    void shouldFailForUnsupportedOperation() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);

        exchange.getMessage().setBody(mapper.readTree("{}"));

        Assertions.assertThrows(UnsupportedOperationException.class, () -> processor.process(Ddb2Operations.BatchGetItems.name(), exchange));
    }

    private void assertAttributeValueMap(Map<String, AttributeValue> attributeValueMap) {
        Assertions.assertEquals(6L, attributeValueMap.size());
        Assertions.assertEquals(AttributeValue.builder().s("Rajesh Koothrappali").build(), attributeValueMap.get("name"));
        Assertions.assertEquals(AttributeValue.builder().n("29").build(), attributeValueMap.get("age"));
        Assertions.assertEquals(AttributeValue.builder().ss("batman", "spiderman", "wonderwoman").build(), attributeValueMap.get("super-heroes"));
        Assertions.assertEquals(AttributeValue.builder().ns("5", "3", "9", "1").build(), attributeValueMap.get("issues"));
        Assertions.assertEquals(AttributeValue.builder().nul(true).build(), attributeValueMap.get("girlfriend"));
        Assertions.assertEquals(AttributeValue.builder().bool(true).build(), attributeValueMap.get("doctorate"));
    }

    private void assertAttributeValueUpdateMap(Map<String, AttributeValueUpdate> attributeValueMap) {
        Assertions.assertEquals(6L, attributeValueMap.size());
        Assertions.assertEquals(AttributeValueUpdate.builder().value(AttributeValue.builder().s("Rajesh Koothrappali").build()).action(AttributeAction.PUT).build(), attributeValueMap.get("name"));
        Assertions.assertEquals(AttributeValueUpdate.builder().value(AttributeValue.builder().n("29").build()).action(AttributeAction.PUT).build(), attributeValueMap.get("age"));
        Assertions.assertEquals(AttributeValueUpdate.builder().value(AttributeValue.builder().ss("batman", "spiderman", "wonderwoman").build()).action(AttributeAction.PUT).build(), attributeValueMap.get("super-heroes"));
        Assertions.assertEquals(AttributeValueUpdate.builder().value(AttributeValue.builder().ns("5", "3", "9", "1").build()).action(AttributeAction.PUT).build(), attributeValueMap.get("issues"));
        Assertions.assertEquals(AttributeValueUpdate.builder().value(AttributeValue.builder().nul(true).build()).action(AttributeAction.PUT).build(), attributeValueMap.get("girlfriend"));
        Assertions.assertEquals(AttributeValueUpdate.builder().value(AttributeValue.builder().bool(true).build()).action(AttributeAction.PUT).build(), attributeValueMap.get("doctorate"));
    }
}