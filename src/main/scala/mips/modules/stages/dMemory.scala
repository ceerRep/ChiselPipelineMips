package mips.modules.stages

import chisel3._
import chisel3.util._
import mips.DataMemoryBlackBox
import mips.modules.BypassUnit
import mips.util.BypassRegData._
import mips.util.Control.{dm_write_from_reg2, reg_write_from_dm}
import mips.util._

class PipelineMemoryResult extends Bundle {
  val pc = SInt(32.W)
  val pcChanged = Bool()
  val inst = new Instruction
  val controlSignal = new ControlSignal
  val regReadId = new GPR.RegisterReadId
  val regReadData = new GPR.RegisterReadData
  val dmAddr = UInt(32.W)
  val wRegId = UInt(5.W)
  val wRegDataReady = Bool()
  val wRegData = UInt(32.W)
  val bubbled = Bool()
}

class dMemory extends Module {
  val io = IO(new Bundle {
    val pipelineExecutionResult = Input(new PipelineExecutionResult)

    val pipelineMemoryResult = Output(new PipelineMemoryResult)
    val dataFromMem = Output(new BypassRegData)
  })
  val dataFromMem = Wire(new BypassRegData)
  io.dataFromMem := dataFromMem

  val pipelineMemoryResult = RegInit(0.U.asTypeOf(new PipelineMemoryResult))
  io.pipelineMemoryResult := pipelineMemoryResult

  val hazardStall = Wire(Vec(2, Bool()))
  val stageDatas = Wire(bypassRegDatas)
  stageDatas(0) := dataFromMem
  stageDatas(1) := bypassNull
  stageDatas(2) := bypassNull

  val bu0 = Module(new BypassUnit)
  bu0.io.pcChanged := io.pipelineExecutionResult.pcChanged
  bu0.io.regId := io.pipelineExecutionResult.regReadId.id1
  bu0.io.origData := io.pipelineExecutionResult.regReadData.data1
  bu0.io.datas := stageDatas
  val regReadData1 = bu0.io.bypassData
  hazardStall(0) := bu0.io.stall

  val bu1 = Module(new BypassUnit)
  bu1.io.pcChanged := io.pipelineExecutionResult.pcChanged
  bu1.io.regId := io.pipelineExecutionResult.regReadId.id2
  bu1.io.origData := io.pipelineExecutionResult.regReadData.data2
  bu1.io.datas := stageDatas
  val regReadData2 = bu1.io.bypassData
  hazardStall(1) := bu1.io.stall

  val regData = Wire(new GPR.RegisterReadData)
  regData.data1 := regReadData1
  regData.data2 := regReadData2

  assert((hazardStall(0) || hazardStall(1)) === 0.B)

  val stall = io.pipelineExecutionResult.bubbled

  val dm = Module(new DataMemoryBlackBox)
  dm.io.clock := clock
  dm.io.addr := io.pipelineExecutionResult.dmAddr
  dm.io.read_size := io.pipelineExecutionResult.controlSignal.dmReadSize
  dm.io.read_sign_extend := io.pipelineExecutionResult.controlSignal.dmReadSigned
  dm.io.write_size := Mux(stall, 0.U, io.pipelineExecutionResult.controlSignal.dmWriteSize)
  dm.io.din := Mux(io.pipelineExecutionResult.controlSignal.dmWriteDataFrom === dm_write_from_reg2,
    regData.data2,
    0.U
  )
  dm.io.pc := io.pipelineExecutionResult.pc

  val passBubble = io.pipelineExecutionResult.bubbled

  when (!stall) {
    pipelineMemoryResult.pc := io.pipelineExecutionResult.pc
    pipelineMemoryResult.pcChanged := io.pipelineExecutionResult.pcChanged
    pipelineMemoryResult.inst := io.pipelineExecutionResult.inst
    pipelineMemoryResult.controlSignal := io.pipelineExecutionResult.controlSignal
    pipelineMemoryResult.regReadId := io.pipelineExecutionResult.regReadId
    pipelineMemoryResult.regReadData := io.pipelineExecutionResult.regReadData
    pipelineMemoryResult.dmAddr := io.pipelineExecutionResult.dmAddr
    pipelineMemoryResult.wRegId := io.pipelineExecutionResult.wRegId

    when (io.pipelineExecutionResult.controlSignal.wRegEnabled) {
      when (io.pipelineExecutionResult.wRegDataReady) {
        pipelineMemoryResult.wRegDataReady := 1.B
        pipelineMemoryResult.wRegData := io.pipelineExecutionResult.wRegData
      }.elsewhen(io.pipelineExecutionResult.controlSignal.wRegDataFrom === reg_write_from_dm) {
        pipelineMemoryResult.wRegDataReady := 1.B
        pipelineMemoryResult.wRegData := dm.io.dout
      }.otherwise{
        pipelineMemoryResult.wRegDataReady := 0.B
        pipelineMemoryResult.wRegData := 0x123455AA.U
      }
    }.otherwise {
      pipelineMemoryResult.wRegDataReady := 1.B
      pipelineMemoryResult.wRegData := 0x0DE0BEEF.U
    }
  }

  pipelineMemoryResult.bubbled := passBubble

  when (pipelineMemoryResult.bubbled) {
    dataFromMem.regId := GPR.ZERO
    dataFromMem.dataReady := 1.B
    dataFromMem.data := 0.U
  }.otherwise {
    dataFromMem.regId := pipelineMemoryResult.wRegId
    dataFromMem.dataReady := pipelineMemoryResult.wRegDataReady
    dataFromMem.data := pipelineMemoryResult.wRegData
  }

}

object dMemory extends App {
  //  (new chisel3.stage.ChiselStage).emitVerilog(new GCD,args.union(Array("-td","v")).distinct)
  (new chisel3.stage.ChiselStage).emitVerilog(new dMemory, Array("-td", "v"))
}