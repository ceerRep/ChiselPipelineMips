package mips

import chisel3._
import chisel3.experimental._
import chisel3.util.HasBlackBoxResource

class InstMemoryBlackBox extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val addr = Input(UInt(32.W))
    val dout = Output(UInt(32.W))
  })

  addResource("/InstMemoryBlackBox.v")
}
