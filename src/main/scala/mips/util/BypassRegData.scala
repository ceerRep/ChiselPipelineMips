package mips.util

import chisel3._

class BypassRegData extends Bundle {
  val regId = UInt(5.W)
  val dataReady = Bool()
  val data = UInt(32.W)
}

object BypassRegData {
  val stallStageCount = 3
  def bypassNull: BypassRegData = {
    val data = Wire(new BypassRegData)
    data.regId := GPR.ZERO
    data.dataReady := 1.B
    data.data := 0.U(32.W)
    data
  }
  def bypassRegDatas: Vec[BypassRegData] = Vec(stallStageCount, new BypassRegData)
}