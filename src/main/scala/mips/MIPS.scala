// See README.md for license details.

package mips

import chisel3._
import mips.modules.{BypassModule, RegisterSet, Memory}
import mips.modules.stages._

class MIPS extends Module {
  val io = IO(new Bundle {
  })

  val gpr = Module(new RegisterSet)

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

  aFetchStage.io.decodeSuspend := bDecodeStage.io.decodeSuspend
  aFetchStage.io.jump := bDecodeStage.io.jump
  aFetchStage.io.jumpValue := bDecodeStage.io.jumpValue
  aFetchStage.io.mem <> memory.io.readonly

  bDecodeStage.io.pipelineFetchResult := aFetchStage.io.pipelineFetchResult
  bDecodeStage.io.suspendOnExecuation := cExecutionStage.io.suspendOnExecution
  bDecodeStage.io.suspendFromExecuation := cExecutionStage.io.suspendFromExecution
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
