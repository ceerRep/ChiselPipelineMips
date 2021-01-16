package mips

import chisel3._
import chisel3.experimental._
import chisel3.util.HasBlackBoxResource

class DataMemoryBlackBox extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val addr = Input(UInt(32.W))
    val din = Input(UInt(32.W))
    val write_size = Input(UInt(2.W))
    val read_size = Input(UInt(2.W))
    val read_sign_extend = Input(Bool())
    val pc = Input(SInt(32.W))
    val dout = Output(UInt(32.W))
  })

  addResource("/DataMemoryBlackBox.sv")
}
