package mips.modules.stages

import chisel3._
import mips.InstMemoryBlackBox
import mips.modules.PCControl
import mips.util.pipeline._
import mips.util.{InstructionParser, Instruction}

class aFetch extends Module {
  val io = IO(new Bundle {
    val decodeSuspend = Input(Bool())
    val jump = Input(Bool())
    val jumpValue = Input(SInt(32.W))
    val pipelineFetchResult = Output(new FetchBundle)

    val mem = new Bundle {
      val addr = Output(UInt(32.W))
      val readSize = Output(UInt(2.W))
      val readSign = Output(Bool())

      val readData = Input(UInt(32.W))
    }
  })

  val pipelineFetchResult = RegInit(0.U.asTypeOf(new FetchBundle))
  io.pipelineFetchResult := pipelineFetchResult

  val pc = Module(new PCControl)
  val im = Module(new InstMemoryBlackBox)

  pc.io.jump := io.jump
  pc.io.jumpValue := io.jumpValue
  pc.io.suspend := io.decodeSuspend

  io.mem.addr := pc.io.value.asUInt()
  io.mem.readSize := 3.U
  io.mem.readSign := 0.B

  when (!reset.asBool && !io.decodeSuspend) {
    pipelineFetchResult.inst := InstructionParser.parseInstruction(io.mem.readData)
    pipelineFetchResult.pc := pc.io.value
    pipelineFetchResult.pcMagic := pc.io.magic
  }
}

object aFetch extends App {
  //  (new chisel3.stage.ChiselStage).emitVerilog(new GCD,args.union(Array("-td","v")).distinct)
  (new chisel3.stage.ChiselStage).emitVerilog(new aFetch,Array("-td","v"))
}