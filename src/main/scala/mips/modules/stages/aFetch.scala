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

    val mem = new Bundle {
      val addr = Output(UInt(32.W))
      val readSize = Output(UInt(2.W))
      val readSign = Output(Bool())

      val readData = Input(UInt(32.W))
    }
  })

  val pipelineFetchResult = RegInit(0.U.asTypeOf(new PipelineFetchResult))
  io.pipelineFetchResult := pipelineFetchResult

  val pc = Module(new PC)
  val im = Module(new InstMemoryBlackBox)

  pc.io.jump := io.jump
  pc.io.jumpValue := io.jumpValue
  pc.io.stall := io.decodeStall

  io.mem.addr := pc.io.value.asUInt()
  io.mem.readSize := 3.U
  io.mem.readSign := 0.B

  when (!(reset.asBool()) && !io.decodeStall) {
    pipelineFetchResult.inst := InstParse.parseInstruction(io.mem.readData)
    pipelineFetchResult.pc := pc.io.value
    pipelineFetchResult.pcChanged := pc.io.valueChanged
  }
}

object aFetch extends App {
  //  (new chisel3.stage.ChiselStage).emitVerilog(new GCD,args.union(Array("-td","v")).distinct)
  (new chisel3.stage.ChiselStage).emitVerilog(new aFetch,Array("-td","v"))
}