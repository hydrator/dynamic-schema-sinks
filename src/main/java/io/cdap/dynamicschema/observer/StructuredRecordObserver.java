/*
 * Copyright © 2017 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.dynamicschema.observer;

import io.cdap.cdap.api.data.format.StructuredRecord;
import io.cdap.cdap.api.data.schema.Schema;
import io.cdap.dynamicschema.api.Observer;
import io.cdap.dynamicschema.api.ObserverException;
import io.cdap.dynamicschema.api.StructuredRecordVisitor;
import io.cdap.dynamicschema.api.VisitorException;
import org.apache.hadoop.hbase.util.Bytes;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * Structured Record Observer.
 */
public final class StructuredRecordObserver implements Observer<StructuredRecord> {
  private final StructuredRecordVisitor visitor;

  public StructuredRecordObserver(StructuredRecordVisitor visitor) {
    this.visitor = visitor;
  }

  public void traverse(StructuredRecord record) throws ObserverException {
    traverse(record, 0);
  }

  private void traverse(StructuredRecord record, int depth) throws ObserverException {
    try {
      if (!visitor.visit(depth, record.getSchema().getRecordName(), null, record)) {
        return;
      }

      for (Schema.Field field : record.getSchema().getFields()) {
        Schema.Type type;
        if (!field.getSchema().isNullable()) {
          type = field.getSchema().getType();
        } else {
          type = field.getSchema().getNonNullable().getType();
        }

        String name = field.getName();

        boolean exit = false;
        switch(type) {
          case INT:
            if(!visitor.visit(depth, name, field, (Integer) record.get(name))) {
              exit = true;
            }
            break;

          case FLOAT:
            if(!visitor.visit(depth, name, field, (Float) record.get(name))) {
              exit = true;
            }
            break;

          case DOUBLE:
            if(!visitor.visit(depth, name, field, (Double) record.get(name))) {
              exit = true;
            }
            break;

          case LONG:
            if(!visitor.visit(depth, name, field, (Long) record.get(name))) {
              exit = true;
            }
            break;

          case BOOLEAN:
            if(!visitor.visit(depth, name, field, (Boolean) record.get(name))) {
              exit = true;
            }
            break;

          case STRING:
            if(!visitor.visit(depth, name, field, (String) record.get(name))) {
              exit = true;
            }
            break;

          case BYTES:
            Object val = record.get(field.getName());
            if(val instanceof ByteBuffer) {
              if(!visitor.visit(depth, name, field, Bytes.toBytes((ByteBuffer) val))) {
                exit = true;
              }
            } else {
              if(!visitor.visit(depth, name, field, (byte[])((byte[])val))) {
                exit = true;
              }
            }
            break;

          case NULL:
            if(!visitor.visit(depth, name, field)) {
              exit = true;
            }
            break;

          case MAP:
            if(!visitor.visit(depth, name, field, (Map<String, String>) record.get(name))){
              exit = true;
            }
            break;

          case ARRAY:
            List<StructuredRecord> rs = (List) record.get(name);
            for (StructuredRecord r : rs ) {
              if (!visitor.visit(depth + 1, name, field, r)) {
                exit = true;
                break;
              }
            }
            break;
        }
        if (exit) {
          break;
        }
      }
    } catch (VisitorException e) {
      throw new ObserverException(e.getMessage());
    }
  }
}
