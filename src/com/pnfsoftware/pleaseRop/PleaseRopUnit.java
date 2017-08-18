package com.pnfsoftware.pleaseRop;

import com.pnfsoftware.pleaseRop.MetaUnit;
import com.pnfsoftware.jeb.core.events.J;
import com.pnfsoftware.jeb.core.events.JebEvent;
import com.pnfsoftware.jeb.core.output.IUnitDocumentPresentation;
import com.pnfsoftware.jeb.core.output.UnitRepresentationAdapter;
import com.pnfsoftware.jeb.core.output.tree.ITreeDocument;
import com.pnfsoftware.jeb.core.output.tree.impl.Node;
import com.pnfsoftware.jeb.core.output.tree.impl.StaticTreeDocument;
import com.pnfsoftware.jeb.core.units.INativeCodeUnit;
import com.pnfsoftware.jeb.core.units.IUnit;
import com.pnfsoftware.jeb.core.units.code.ICodeUnit;
import com.pnfsoftware.jeb.core.units.code.IInstruction;
import com.pnfsoftware.jeb.core.units.code.IInstructionOperand;
import com.pnfsoftware.jeb.core.units.code.asm.cfg.BasicBlock;
import com.pnfsoftware.jeb.core.units.code.asm.cfg.CFG;
import com.pnfsoftware.jeb.core.units.code.asm.decompiler.IEConverter;
import com.pnfsoftware.jeb.core.units.code.asm.decompiler.IEGlobalContext;
import com.pnfsoftware.jeb.core.units.code.asm.decompiler.IERoutineContext;
import com.pnfsoftware.jeb.core.units.code.asm.decompiler.INativeDecompilerUnit;
import com.pnfsoftware.jeb.core.units.code.asm.decompiler.ir.IEAssign;
import com.pnfsoftware.jeb.core.units.code.asm.decompiler.ir.IEGeneric;
import com.pnfsoftware.jeb.core.units.code.asm.decompiler.ir.IEStatement;
import com.pnfsoftware.jeb.core.units.code.asm.decompiler.ir.IEVar;
import com.pnfsoftware.jeb.core.units.code.asm.items.INativeInstructionItem;
import com.pnfsoftware.jeb.core.units.code.asm.items.INativeMethodItem;
import com.pnfsoftware.jeb.core.units.code.asm.processor.IInstructionOperandGeneric;
import com.pnfsoftware.jeb.core.util.DecompilerHelper;
import com.pnfsoftware.jeb.util.logging.GlobalLog;
import com.pnfsoftware.jeb.util.logging.ILogger;
import java.util.*;

public class PleaseRopUnit {

    // Needed to print to JEB's console.
    private static final ILogger logger = GlobalLog.getLogger(PleaseRopUnit.class);
    // 
    private IEConverter<?> converter;

    private INativeDecompilerUnit<?> decompiler;

    private INativeCodeUnit<IInstruction> codeUnit;

    private Map<String, List<String>> gadgetMap = new HashMap<String, List<String>>();
    
    private String artifactName;
    
    private Map<String, Long> gadgetAddrMap = new HashMap<String, Long>();
    
    private List<Long> gadgetBlacklist = new ArrayList<Long>();
    
    private int pcId;

    @SuppressWarnings("unchecked")
    // Thread started by the plugin.
    PleaseRopUnit(List<ICodeUnit> codeUnits) {

        try {
            
            List<String> gadgetList = new ArrayList<>();
            
            for(int i = 0; i < codeUnits.size(); i++) {
                logger.info("Looking for gadgets...");
                codeUnit = (INativeCodeUnit<IInstruction>)codeUnits.get(i);

                IUnit parentUnit = (IUnit)codeUnit.getParent();
                
                artifactName = parentUnit.getName();
                
                decompiler = (INativeDecompilerUnit<?>)DecompilerHelper.getDecompiler(codeUnit);

                if(decompiler == null) {

                    logger.info("There is no decompiler available for code unit %s", codeUnit.toString());
                    return;

                }

                converter = decompiler.getConverter();
                
                IEVar pcRegister = converter.getProgramCounter();
                
                pcId = pcRegister.getId();

                List<CFG<IEStatement>> cfgList = getAllConvertedCfgs(codeUnit);

                for(CFG<IEStatement> cfg: cfgList) {

                    if(cfg != null) {
                        // Iterate through each basic blocks.
                        for(int j = 0; j < cfg.size(); j++) {

                            BasicBlock<IEStatement> basicBlock = cfg.get(j);

                            List<List<IEStatement>> gadgetsList = getGadgetsList(basicBlock);

                            for(List<IEStatement> gadgetStatements: gadgetsList) {

                                if(gadgetStatements.size() == 0) {

                                    continue;

                                }

                                List<Long> alreadyAddedAddresses = new ArrayList<>();
                                String gadgetLine = "";

                                // Adding the address of the gadget.
                                IEStatement firstGadgetStatement = gadgetStatements.get(0);
                                List<Long> firstStatementAddresses = new ArrayList<Long>(
                                        firstGadgetStatement.getLowerLevelAddresses());
                                long firstInstructionAddress = firstStatementAddresses.get(0);
                                
                                for(int l = 0; l < gadgetStatements.size() ; l++) {

                                    IEStatement gadgetStatement = gadgetStatements.get(l);
                                    List<Long> gadgetStatementAddresses = new ArrayList<Long>(
                                            gadgetStatement.getLowerLevelAddresses());
                                    gadgetLine = addStatementToGadgetLine(gadgetStatementAddresses, gadgetLine,
                                            alreadyAddedAddresses);

                                }
                                
                                if (gadgetLine != null) {
                                    
                                    if (!gadgetList.contains(gadgetLine)) {
                                    
                                        gadgetAddrMap.put(gadgetLine, firstInstructionAddress);
                                        gadgetList.add(gadgetLine);
                                    }
                                }
                            }
                        }
                    }
                }
                Collections.sort(gadgetList);
                
                List<String> gadgetsWithAddresses = new ArrayList<>();
                
                for (String gadgetLine: gadgetList) {
                    
                    Long gadgetAddress = gadgetAddrMap.get(gadgetLine);
                    
                    String gadgetWithAddress =  "0x" + gadgetAddress.toString() + ": " + gadgetLine;
                    
                    gadgetsWithAddresses.add(gadgetWithAddress);
                    
                }
                
                gadgetMap.put(artifactName, gadgetsWithAddresses);
            }
            
            
            ITreeDocument treeDocument = createTree(gadgetMap);
            IUnitDocumentPresentation gadgetPresentation =  new UnitRepresentationAdapter(
                    200,
                    "ROP gadgets",
                    false,
                    treeDocument
                    );

            for (ICodeUnit codeUnit: codeUnits) {

                IUnit parentUnit = (IUnit)codeUnit.getParent();

                MetaUnit metaUnit = new MetaUnit("PleaseROP", "ROP Gadgets", gadgetPresentation, codeUnit);

                logger.info("Name: %s", parentUnit.getName());

                metaUnit.setParent(parentUnit);

                parentUnit.addChild(metaUnit);

                // Refreshes the unit with the new presentation
                codeUnit.notifyListeners(new JebEvent(J.UnitChange));
            }
            
        }

        catch(NullPointerException e) {
            StackTraceElement[] stackTrace = e.getStackTrace();
            for(int i = 0; i < stackTrace.length; i++) {
                logger.debug("%s", stackTrace[i].toString());
            }

        }

    }
    
    /*
     * Creates a TreeDocument and adds it the every artifact.
     * 
     * @param tableMap Map of the callers and its number of call occurrences.
     * @return TableDocument that will be added as a new presentation. 
     * */
    private ITreeDocument createTree(Map<String, List<String>> treeMap) {
        
        List<Node> root = new ArrayList<>();                // Each caller has a row
        Set<String> artifactSet = treeMap.keySet();        // Set of each caller's name.
        Object[] artifactNames = artifactSet.toArray(); // Set to Array.
        
        
        // Create a row for each caller of the selected function.
        for (int i =0; i < treeMap.size(); i++) {
            
            String artifactName = artifactNames[i].toString();
            Node artifact = new Node(artifactName);
            logger.info("Creating node for %s", artifact);
            
            List<String> subitemList = treeMap.get(artifactName);
            
            for (String subitem: subitemList) {
            
                Node itemNode = new Node(subitem);    
                artifact.addChild(itemNode);
                
            }
            root.add(artifact);
            
        }
        return new StaticTreeDocument(root);
        
    }

    /**
     * Gets a list of all the gadgets in a basic block.
     * 
     * @param basicBlock IEStatement basic block.
     * 
     * @return List of all the gadgets in that basic block. 
     * 
     * */

    private List<List<IEStatement>> getGadgetsList(BasicBlock<IEStatement> basicBlock) {

        List<List<IEStatement>> gadgetsList = new ArrayList<>();

        //Iterate through each statements.
        for(int k = basicBlock.size() - 1; k >= 0; k--) {

            List<IEStatement> gadgetStatements = new ArrayList<>();

            if(k == 0) {
                continue;
            }

            IEStatement statement = basicBlock.get(k);

            int destinationId = getDestinationOperandId(statement);

            if(destinationId == pcId) {

                do {

                    gadgetStatements.add(statement);
                    k--;
                    statement = basicBlock.get(k);
                    destinationId = getDestinationOperandId(statement);

                }
                while(destinationId != pcId && k > 0);
            }

            else {
            }

            if(gadgetStatements.size() != 0) {
                // The gadget list is reversed because we started from the end in the previous step.
                // We now have a list of gadgets in the same order as the assembly.
                Collections.reverse(gadgetStatements);
                gadgetsList.add(gadgetStatements);
            }

        }

        return gadgetsList;
    }
    

    /**
     * Takes a list of instruction addresses and formats it 
     * to a the respective mnemonics, operands and address.
     * 
     * @param codeUnit Code unit that the gadget belongs to.
     * @param gadgetStatementAddresses 
     * 
     * */

    private String addStatementToGadgetLine(List<Long> gadgetStatementAddresses, String gadgetLine,
            List<Long> alreadyAddedAddresses) {

        // Every statement can potentially resolve to one or more instructions.
        for(Long instructionAddress: gadgetStatementAddresses) {

            // Every instruction can resolve to multiple statements and we don't want duplicate instructions.
            if(!alreadyAddedAddresses.contains(instructionAddress)) {

                // Blacklist the address to eliminate duplicates.
                alreadyAddedAddresses.add(instructionAddress);

                // Getting the appropriate instruction for the statement.
                INativeInstructionItem instructionItem = (INativeInstructionItem)codeUnit
                        .getNativeItemAt(instructionAddress);
                IInstruction instruction = instructionItem.getInstruction();

                IInstructionOperand[] instructionOperands = instruction.getOperands();

                if (instructionOperands.length == 1) {

                    IInstructionOperand instructionOperand = instructionOperands[0];
                    if (instructionOperand instanceof IInstructionOperandGeneric) {

                        int operandType = ((IInstructionOperandGeneric)instructionOperand).getOperandType();

                        
                        
                        if (operandType == IInstructionOperandGeneric.TYPE_RELADDR) {

                            return null;

                        }

                    }
                }
                
                String formattedInstruction = instruction.format(null);
                gadgetLine += formattedInstruction; 
                gadgetLine += "; ";
            }
        }
        return gadgetLine;
    }

    /**
     * Gets a list of all the decompiled CFGs.
     * 
     * @param codeUnit
     * @param CFG_TAG Tag of the pipeline step 
     * @return List of all the converted CFGs.
     * 
     * */

    private List<CFG<IEStatement>> getDecompiledCFGs(INativeCodeUnit<IInstruction> codeUnit, String CFG_TAG) {

        List<CFG<IEStatement>> cfgList = new ArrayList<>();

        // Getting all the routine contexts from the global context.
        IEGlobalContext globalContext = converter.getGlobalContext();
        List<IERoutineContext> routineCtxList = (List<IERoutineContext>)globalContext.getRoutineContexts();

        for(IERoutineContext routineCtx: routineCtxList) {
            CFG<IEStatement> cfg = routineCtx.getCfg(CFG_TAG);
            cfgList.add(cfg);
        }

        return cfgList;
    }

    /**
     * Gets a list of all the CFGs right after the conversion in IR.
     * 
     * @param codeUnit The native code unit that you want the converted CFGs.
     * @return List of all the converted CFGs.
     * */

    private List<CFG<IEStatement>> getAllConvertedCfgs(INativeCodeUnit<IInstruction> codeUnit) {

        List<CFG<IEStatement>> convertedCFGs = new ArrayList<>();
        List<? extends INativeMethodItem> methods = codeUnit.getInternalMethods();

        for(INativeMethodItem method: methods) {

            String methodAddress = method.getAddress();

            if(decompiler.canDecompile(methodAddress)) {

                IERoutineContext routineContext = converter.convert(method);
                CFG<IEStatement> cfg = routineContext.getCfg();
                convertedCFGs.add(cfg);
            }
        }

        return convertedCFGs;

    }

    /**
     * Gets the ID of the destination variable to check the type of it.
     * If the IEStatement is not an IEAssign, returns 0x10000000.
     * 
     * @param statement IEStatement that should be a IEAssign (to have a destination operand)
     * @return id (0x10000000 if not applicable).
     *
     * */

    private int getDestinationOperandId(IEStatement statement) {

        int id = 0x10000000; // Invalid ID.

        if(statement instanceof IEAssign) {

            IEGeneric destinationOperand = ((IEAssign)statement).getDstOperand();

            if(destinationOperand instanceof IEVar) {

                id = ((IEVar)destinationOperand).getId();

            }
        }

        return id;

    }

    /**
     * Gets the ID of the destination variable to check the type of it.
     * If the IEStatement is not an IEAssign, returns 0x10000000.
     * 
     * @param statement IEStatement that should be a IEAssign (to have a destination operand)
     * @return id (0x10000000 if not applicable).
     *
     * */

    private int getSourceOperandId(IEStatement statement) {

        int id = 0x10000000; // Invalid ID.

        if(statement instanceof IEAssign) {

            IEGeneric destinationOperand = ((IEAssign)statement).getSrcOperand();

            if(destinationOperand instanceof IEVar) {

                id = ((IEVar)destinationOperand).getId();

            }
        }

        return id;

    }
}