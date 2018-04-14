/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingjdbc.core.parsing.parser.context.condition;

import io.shardingjdbc.core.parsing.parser.expression.SQLNumberExpression;
import io.shardingjdbc.core.routing.sharding.GeneratedKey;
import lombok.Getter;
import lombok.ToString;

/**
 * Generated key condition.
 * 
 * @author zhangliang
 * @author maxiaoguang
 */
@Getter
@ToString
public final class GeneratedKeyCondition extends Condition {
    
    private final Column column;
    
    private final int index;
    
    private final Number value;
    
    public GeneratedKeyCondition(final GeneratedKey generatedKey) {
        this(generatedKey.getColumn(), generatedKey.getIndex(), generatedKey.getValue());
    }
    
    public GeneratedKeyCondition(final Column column, final int index, final Number value) {
        super(column, new SQLNumberExpression(value));
        this.column = column;
        this.index = index;
        this.value = value;
    }
}
