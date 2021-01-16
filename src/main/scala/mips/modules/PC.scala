package mips.modules

import chisel3._
import chisel3.util._

class PC extends Module {

  val io = IO(new Bundle {
    val stall = Input(Bool())
    val jump = Input(Bool())
    val jumpValue = Input(SInt(32.W))
    val value = Output(SInt(32.W))
    val valueChanged = Output(Bool())
  })

  val value = RegInit(SInt(32.W), (0x00003000 - 4).S(32.W))
  val valueChanged = RegInit(Bool(), 1.B)
  val nextValue = Wire(SInt(32.W))

  nextValue := Mux(io.jump, io.jumpValue, value + 4.S)

  io.value := value
  io.valueChanged := valueChanged

  when (!io.stall) {
    value := nextValue
    valueChanged := !valueChanged
  }
}

object PC extends App {
  object JumpType {
    val near :: far :: relative :: Nil = Enum(3)
  }
  //  (new chisel3.stage.ChiselStage).emitVerilog(new GCD,args.union(Array("-td","v")).distinct)
  (new chisel3.stage.ChiselStage).emitVerilog(new PC,Array("-td","v"))
}