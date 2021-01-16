// See README.md for license details.

package mips

import chisel3._
import mips.modules.GeneralPurposeRegisters
import mips.modules.stages._
import mips.util.{BypassRegData, GPR}

/**
  * Compute GCD using subtraction method.
  * Subtracts the smaller from the larger until register y is zero.
  * value in register x is then the GCD
  */
class MIPS extends Module {
  val io = IO(new Bundle {
  })

  val gpr = Module(new GeneralPurposeRegisters)
  val fetch = Module(new aFetch)
  val decode = Module(new bDecode)
  val execution = Module(new cExecution)
  val memory = Module(new dMemory)
  val writeBack = Module(new eWriteBack)

  gpr.io.readId := decode.io.regReadId
  gpr.io.writeId := writeBack.io.wRegId
  gpr.io.we := writeBack.io.wRegEnabled
  gpr.io.din := writeBack.io.wRegData
  gpr.io.pcWrite := memory.io.pipelineMemoryResult.pc

  fetch.io.decodeStall := decode.io.decodeStall
  fetch.io.jump := decode.io.jump
  fetch.io.jumpValue := decode.io.jumpValue

  decode.io.pipelineFetchResult := fetch.io.pipelineFetchResult
  decode.io.stallOnExecuation := execution.io.stallOnExecution
  decode.io.stallFromExecuation := execution.io.stallFromExecution
  decode.io.dataFromExec := execution.io.dataFromExec
  decode.io.dataFromMem := memory.io.dataFromMem
  decode.io.regReadData := gpr.io.readData

  execution.io.pipelineDecodeResult := decode.io.pipelineDecodeResult
  execution.io.dataFromMem := memory.io.dataFromMem

  memory.io.pipelineExecutionResult := execution.io.pipelineExecutionResult

  writeBack.io.pipelineMemoryResult := memory.io.pipelineMemoryResult
}

object MIPS extends App {
//  (new chisel3.stage.ChiselStage).emitVerilog(new GCD,args.union(Array("-td","v")).distinct)
  (new chisel3.stage.ChiselStage).emitVerilog(new MIPS,Array("-td","v"))
}
