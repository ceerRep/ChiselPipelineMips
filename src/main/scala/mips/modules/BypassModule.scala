package mips.modules

import chisel3._
import chisel3.util._
import mips.util.BypassRegData._
import mips.util.GPR

class BypassModule extends Module {
  val io = IO(new Bundle {
    val stageDatas = Input(bypassRegDatas)
    val bypassQueries = Vec(stallStageCount, new Bundle {
      val pcChanged = Input(Bool())
      val regReadId = Input(new GPR.RegisterReadId)
      val origRegData = Input(new GPR.RegisterReadData)
      val bypassData = Output(Vec(2, new Bundle {
        val data = UInt(32.W)
        val stall = Bool()
      }))
    })
  })

  val currentBypassData = Wire(Vec(stallStageCount, Vec(2, new Bundle {
    val data = UInt(32.W)
    val stall = Bool()
  })))

  for (i <- 0 until stallStageCount) {
    val query = io.bypassQueries(i)
    currentBypassData(i)(0).data := Mux(query.regReadId.id1 === GPR.ZERO,
      0.U(32.W),
      MuxCase(query.origRegData.data1,
        (i until stallStageCount)
          .map(x => (io.stageDatas(x).regId === query.regReadId.id1) -> io.stageDatas(x).data)
      ))
    currentBypassData(i)(0).stall := Mux(query.regReadId.id1 === GPR.ZERO,
      0.B,
      MuxCase(0.B,
        (i until stallStageCount)
          .map(x => (io.stageDatas(x).regId === query.regReadId.id1) -> !io.stageDatas(x).dataReady)
      ))

    currentBypassData(i)(1).data := Mux(query.regReadId.id2 === GPR.ZERO,
      0.U(32.W),
      MuxCase(query.origRegData.data2,
        (i until stallStageCount)
          .map(x => (io.stageDatas(x).regId === query.regReadId.id2) -> io.stageDatas(x).data)
      ))
    currentBypassData(i)(1).stall := Mux(query.regReadId.id2 === GPR.ZERO,
      0.B,
      MuxCase(0.B,
        (i until stallStageCount)
          .map(x => (io.stageDatas(x).regId === query.regReadId.id2) -> !io.stageDatas(x).dataReady)
      ))
  }

  withClock((!clock.asBool).asClock) {
    val storedBypassData = RegInit(0.U.asTypeOf(chiselTypeOf(currentBypassData)))
    val storedPCChanged = RegInit(Fill(2 * stallStageCount, 1.U(1.W))
      .asTypeOf(Vec(stallStageCount, Vec(2, Bool()))))

    for (i <- 0 until stallStageCount)
      io.bypassQueries(i).bypassData := storedBypassData(i)

    for (i <- 0 until stallStageCount) {
      val query = io.bypassQueries(i)

      for (j <- 0 to 1)
        when (storedPCChanged(i)(j) =/= query.pcChanged ||
          storedBypassData(i)(j).stall =/= currentBypassData(i)(j).stall) {

          storedPCChanged(i)(j) := query.pcChanged
          storedBypassData(i)(j) := currentBypassData(i)(j)
        }
    }
  }
}

object BypassModule extends App {
  //  (new chisel3.stage.ChiselStage).emitVerilog(new GCD,args.union(Array("-td","v")).distinct)
  (new chisel3.stage.ChiselStage).emitVerilog(new BypassModule, Array("-td", "v"))
}
