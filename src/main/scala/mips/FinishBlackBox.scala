package mips

import chisel3._
import chisel3.experimental._
import chisel3.util.HasBlackBoxResource

class FinishBlackBox extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val finish = Input(Bool())
  })
  addResource("/FinishBlackBox.v")
}
