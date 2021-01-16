package mips.modules

import chisel3._
import chisel3.util._
import mips.util.BypassRegData._
import mips.util.GPR

class BypassModule extends Module {
  val io = IO(new Bundle {
    val stageDatas = Input(bypassRegDatas)
    val bypassQueries = Vec(bypassStageCount, new Bundle {
      val pcMagic = Input(Bool())
      val regReadId = Input(GPR.registerReadId)
      val origRegData = Input(GPR.registerReadData)
      val bypassData = Output(Vec(2, new Bundle {
        val data = UInt(32.W)
        val suspend = Bool()
      }))
    })
  })

  val currentBypassData = Wire(Vec(bypassStageCount, Vec(2, new Bundle {
    val data = UInt(32.W)
    val suspend = Bool()
  })))

  for (i <- 0 until bypassStageCount) {
    val query = io.bypassQueries(i)

    for (regInd <- 0 to 1) {
      currentBypassData(i)(regInd).data := Mux(query.regReadId(regInd) === GPR.ZERO,
        0.U(32.W),
        MuxCase(query.origRegData(regInd),
          (i until bypassStageCount)
            .map(x => (io.stageDatas(x).regId === query.regReadId(regInd)) -> io.stageDatas(x).data)
        ))

      currentBypassData(i)(regInd).suspend := Mux(query.regReadId(regInd) === GPR.ZERO,
        0.B,
        MuxCase(0.B,
          (i until bypassStageCount)
            .map(x => (io.stageDatas(x).regId === query.regReadId(regInd)) -> !io.stageDatas(x).dataReady)
        ))
    }
  }

  withClock((!clock.asBool).asClock) {
    val storedBypassData = RegInit(0.U.asTypeOf(chiselTypeOf(currentBypassData)))
    val storedPCMagic = RegInit(Fill(2 * bypassStageCount, 1.U(1.W))
      .asTypeOf(Vec(bypassStageCount, Vec(2, Bool()))))

    for (i <- 0 until bypassStageCount)
      io.bypassQueries(i).bypassData := storedBypassData(i)

    for (i <- 0 until bypassStageCount) {
      val query = io.bypassQueries(i)

      for (j <- 0 to 1)
        when (storedPCMagic(i)(j) =/= query.pcMagic ||
          storedBypassData(i)(j).suspend =/= currentBypassData(i)(j).suspend) {

          storedPCMagic(i)(j) := query.pcMagic
          storedBypassData(i)(j) := currentBypassData(i)(j)
        }
    }
  }
}

object BypassModule extends App {
  //  (new chisel3.stage.ChiselStage).emitVerilog(new GCD,args.union(Array("-td","v")).distinct)
  (new chisel3.stage.ChiselStage).emitVerilog(new BypassModule, Array("-td", "v"))
}
