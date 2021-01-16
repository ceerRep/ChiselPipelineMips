package mips.modules

import chisel3._
import chisel3.util._
import mips.util.BypassRegData.{bypassRegDatas, stallStageCount}
import mips.util.GPR

class BypassUnit extends Module {
  val io = IO(new Bundle {
    val pcChanged = Input(Bool())
    val regId = Input(UInt(5.W))
    val origData = Input(UInt(32.W))
    val datas = Input(bypassRegDatas)
    val bypassData = Output(UInt(32.W))
    val stall = Output(Bool())
  })

  val currentBypassData = Mux(io.regId === GPR.ZERO,
    0.U(32.W),
    MuxCase(io.origData,
      (0 until stallStageCount)
        .map(x => (io.datas(x).regId === io.regId) -> io.datas(x).data)
    ))
  val currentStall = Mux(io.regId === GPR.ZERO,
    0.B,
    MuxCase(0.B,
      (0 until stallStageCount)
      .map(x => (io.datas(x).regId === io.regId) -> !io.datas(x).dataReady)
    ))

  withClock((!clock.asBool).asClock) {
    val bypassData = RegInit(0.U(32.W))
    val stall = RegInit(0.B)
    val prevPCChanged = RegInit(1.B)

    io.bypassData := bypassData
    io.stall := stall

    when (io.pcChanged =/= prevPCChanged || currentStall =/= stall) {
      prevPCChanged := io.pcChanged
      bypassData := currentBypassData
      stall := currentStall
    }
  }
}

object BypassUnit extends App {
  //  (new chisel3.stage.ChiselStage).emitVerilog(new GCD,args.union(Array("-td","v")).distinct)
  (new chisel3.stage.ChiselStage).emitVerilog(new BypassUnit, Array("-td", "v"))
}