package mips.modules.stages

import chisel3._
import mips.InstMemoryBlackBox
import mips.modules.PC
import mips.util.{InstParse, Instruction}

class PipelineFetchResult extends Bundle {
  val pc = SInt(32.W)
  val pcChanged = Bool()
  val inst = new Instruction
}

class aFetch extends Module {
  val io = IO(new Bundle {
    val decodeStall = Input(Bool())
    val jump = Input(Bool())
    val jumpValue = Input(SInt(32.W))
    val pipelineFetchResult = Output(new PipelineFetchResult)
  })

  val pipelineFetchResult = RegInit(0.U.asTypeOf(new PipelineFetchResult))
  io.pipelineFetchResult := pipelineFetchResult

  val pc = Module(new PC)
  val im = Module(new InstMemoryBlackBox)

  pc.io.jump := io.jump
  pc.io.jumpValue := io.jumpValue
  pc.io.stall := io.decodeStall

  im.io.addr := pc.io.value.asUInt() - 0x3000.U

  when (!(reset.asBool()) && !io.decodeStall) {
    pipelineFetchResult.inst := InstParse.parseInstruction(im.io.dout)
    pipelineFetchResult.pc := pc.io.value
    pipelineFetchResult.pcChanged := pc.io.valueChanged
  }
}

object aFetch extends App {
  //  (new chisel3.stage.ChiselStage).emitVerilog(new GCD,args.union(Array("-td","v")).distinct)
  (new chisel3.stage.ChiselStage).emitVerilog(new aFetch,Array("-td","v"))
}