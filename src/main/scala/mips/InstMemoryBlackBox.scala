package mips

import chisel3._
import chisel3.experimental._
import chisel3.util.HasBlackBoxResource

class InstMemoryBlackBox extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val addr0 = Input(UInt(32.W))
    val dout0 = Output(UInt(32.W))

    val addr1 = Input(UInt(32.W))
    val dout1 = Output(UInt(32.W))
  })

  addResource("/InstMemoryBlackBox.v")
}
