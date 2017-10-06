/*
 * JEB Copyright PNF Software, Inc.
 * 
 *     https://www.pnfsoftware.com
 * 
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
 */

package com.pnfsoftware.pleaserop;

import com.pnfsoftware.jeb.core.output.AbstractUnitRepresentation;
import com.pnfsoftware.jeb.core.output.IGenericDocument;
import com.pnfsoftware.jeb.core.output.IUnitDocumentPresentation;
import com.pnfsoftware.jeb.core.output.IUnitFormatter;
import com.pnfsoftware.jeb.core.output.UnitFormatterAdapter;
import com.pnfsoftware.jeb.core.units.AbstractUnit;
import com.pnfsoftware.jeb.core.units.code.ICodeUnit;

/**
 * 
 * 
 * @author Hugo Genesse
 *
 */
public class MetaUnit extends AbstractUnit {
    private IUnitDocumentPresentation presentation_;

    public MetaUnit(String name, String description, IUnitDocumentPresentation presentation, ICodeUnit codeUnit) {
        super(name, description, codeUnit);
        presentation_ = presentation;
    }

    @Override
    public boolean process() {
        setProcessed(true);
        return true;
    }

    @Override
    public IUnitFormatter getFormatter() {
        UnitFormatterAdapter adapter = new UnitFormatterAdapter(new AbstractUnitRepresentation("API Use", true) {
            public IGenericDocument getDocument() {
                return presentation_.getDocument();
            }
        });
        return adapter;
    }
}
