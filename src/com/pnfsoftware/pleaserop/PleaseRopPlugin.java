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

import java.util.List;
import java.util.Map;

import com.pnfsoftware.jeb.core.IEnginesContext;
import com.pnfsoftware.jeb.core.IEnginesPlugin;
import com.pnfsoftware.jeb.core.IOptionDefinition;
import com.pnfsoftware.jeb.core.IPluginInformation;
import com.pnfsoftware.jeb.core.IRuntimeProject;
import com.pnfsoftware.jeb.core.PluginInformation;
import com.pnfsoftware.jeb.core.RuntimeProjectUtil;
import com.pnfsoftware.jeb.core.Version;
import com.pnfsoftware.jeb.core.units.code.ICodeUnit;
import com.pnfsoftware.jeb.util.logging.GlobalLog;
import com.pnfsoftware.jeb.util.logging.ILogger;
import com.pnfsoftware.pleaserop.PleaseRopUnit;

/**
 * PleaseROP plugin entry-point.
 * 
 * @author Hugo Genesse
 * 
 */
public class PleaseRopPlugin implements IEnginesPlugin {
    static final ILogger logger = GlobalLog.getLogger(PleaseRopPlugin.class);

    @Override
    public IPluginInformation getPluginInformation() {
        return new PluginInformation("PleaseROP Plugin", "IR ROP gadget finder", "Hugo Genesse",
                Version.create(1, 0, 1));
    }

    @Override
    public List<? extends IOptionDefinition> getExecutionOptionDefinitions() {
        return null;
    }

    @Override
    public void execute(IEnginesContext context) {
        execute(context, null);
    }

    @Override
    public void execute(IEnginesContext engctx, Map<String, String> executionOptions) {
        IRuntimeProject project = engctx.getProject(0);
        List<ICodeUnit> codeUnits = RuntimeProjectUtil.findUnitsByType(project, ICodeUnit.class, false);
        new PleaseRopUnit(codeUnits);
    }

    @Override
    public void dispose() {
    }
}
