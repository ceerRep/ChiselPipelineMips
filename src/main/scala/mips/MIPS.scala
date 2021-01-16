// See README.md for license details.

package mips

import chisel3._
import mips.modules.{BypassModule, GeneralPurposeRegisters, Memory}
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

  val bypassModule = Module(new BypassModule)

  val memory = Module(new Memory)

  val aFetchStage = Module(new aFetch)
  val bDecodeStage = Module(new bDecode)
  val cExecutionStage = Module(new cExecution)
  val dMemoryStage = Module(new dMemory)
  val eWriteBackStage = Module(new eWriteBack)

  bypassModule.io.stageDatas(0) := bDecodeStage.io.dataFromDecode
  bypassModule.io.stageDatas(1) := cExecutionStage.io.dataFromExec
  bypassModule.io.stageDatas(2) := dMemoryStage.io.dataFromMem

  bypassModule.io.bypassQueries(0) <> bDecodeStage.io.bypass
  bypassModule.io.bypassQueries(1) <> cExecutionStage.io.bypass
  bypassModule.io.bypassQueries(2) <> dMemoryStage.io.bypass

  gpr.io.readId := bDecodeStage.io.regReadId
  gpr.io.writeId := eWriteBackStage.io.wRegId
  gpr.io.we := eWriteBackStage.io.wRegEnabled
  gpr.io.din := eWriteBackStage.io.wRegData
  gpr.io.pcWrite := dMemoryStage.io.pipelineMemoryResult.pc

  aFetchStage.io.decodeStall := bDecodeStage.io.decodeStall
  aFetchStage.io.jump := bDecodeStage.io.jump
  aFetchStage.io.jumpValue := bDecodeStage.io.jumpValue
  aFetchStage.io.mem <> memory.io.readonly

  bDecodeStage.io.pipelineFetchResult := aFetchStage.io.pipelineFetchResult
  bDecodeStage.io.stallOnExecuation := cExecutionStage.io.stallOnExecution
  bDecodeStage.io.stallFromExecuation := cExecutionStage.io.stallFromExecution
  bDecodeStage.io.regReadData := gpr.io.readData

  cExecutionStage.io.pipelineDecodeResult := bDecodeStage.io.pipelineDecodeResult

  dMemoryStage.io.pipelineExecutionResult := cExecutionStage.io.pipelineExecutionResult
  dMemoryStage.io.mem <> memory.io.readwrite

  eWriteBackStage.io.pipelineMemoryResult := dMemoryStage.io.pipelineMemoryResult
}

object MIPS extends App {
  //  (new chisel3.stage.ChiselStage).emitVerilog(new GCD,args.union(Array("-td","v")).distinct)
  (new chisel3.stage.ChiselStage).emitVerilog(new MIPS, Array("-td", "v"))
}
