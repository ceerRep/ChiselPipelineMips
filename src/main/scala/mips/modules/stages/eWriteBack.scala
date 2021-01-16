package mips.modules.stages

import chisel3._
import chisel3.util._
import mips.FinishBlackBox
import mips.util.pipeline._
import mips.util.InstructionByteCode

class eWriteBack extends Module {
  val io = IO(new Bundle {
    val pipelineMemoryResult = Input(new MemoryBundle)

    val wRegId = Output(UInt(5.W))
    val wRegEnabled = Output(Bool())
    val wRegData = Output(UInt(32.W))
  })

  val suspend = io.pipelineMemoryResult.bubbled

  io.wRegId := io.pipelineMemoryResult.wRegId
  io.wRegEnabled := io.pipelineMemoryResult.controlSignal.wRegEnabled && !suspend
  io.wRegData := io.pipelineMemoryResult.wRegData

  val finish = Module(new FinishBlackBox)
  finish.io.finish := io.pipelineMemoryResult.inst.inst === InstructionByteCode.SYSCALL
}

object eWriteBack extends App {
  //  (new chisel3.stage.ChiselStage).emitVerilog(new GCD,args.union(Array("-td","v")).distinct)
  (new chisel3.stage.ChiselStage).emitVerilog(new eWriteBack, Array("-td", "v"))
}