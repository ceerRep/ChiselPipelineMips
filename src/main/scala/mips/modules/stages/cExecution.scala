package mips.modules.stages

import chisel3._
import chisel3.util._
import mips.modules.{ALU, BypassUnit, MDU}
import mips.util.BypassRegData._
import mips.util.Control._
import mips.util._

class PipelineExecutionResult extends Bundle {
  val pc = SInt(32.W)
  val pcChanged = Bool()
  val inst = new Instruction
  val controlSignal = new ControlSignal
  val regReadId = new GPR.RegisterReadId
  val regReadData = new GPR.RegisterReadData
  val wRegId = UInt(5.W)
  val aluResult = UInt(32.W)
  val dmAddr = UInt(32.W)
  val wRegDataReady = Bool()
  val wRegData = UInt(32.W)
  val bubbled = Bool()
}

class cExecution extends Module {
  val io = IO(new Bundle{
    val pipelineDecodeResult = Input(new PipelineDecodeResult)
    val dataFromMem = Input(new BypassRegData)

    val pipelineExecutionResult = Output(new PipelineExecutionResult)
    val stallOnExecution = Output(new Bool())
    val stallFromExecution = Output(new Bool())
    val dataFromExec = Output(new BypassRegData)
  })
  val dataFromExec = Wire(new BypassRegData)
  io.dataFromExec := dataFromExec

  val pipelineExecutionResult = RegInit(0.U.asTypeOf(new PipelineExecutionResult))
  io.pipelineExecutionResult := pipelineExecutionResult

  val hazardStall = Wire(Vec(2, Bool()))
  val stageDatas = Wire(bypassRegDatas)
  stageDatas(0) := dataFromExec
  stageDatas(1) := io.dataFromMem
  stageDatas(2) := bypassNull

  val bu0 = Module(new BypassUnit)
  bu0.io.pcChanged := io.pipelineDecodeResult.pcChanged
  bu0.io.regId := io.pipelineDecodeResult.regReadId.id1
  bu0.io.origData := io.pipelineDecodeResult.regReadData.data1
  bu0.io.datas := stageDatas
  val regReadData1 = bu0.io.bypassData
  hazardStall(0) := bu0.io.stall

  val bu1 = Module(new BypassUnit)
  bu1.io.pcChanged := io.pipelineDecodeResult.pcChanged
  bu1.io.regId := io.pipelineDecodeResult.regReadId.id2
  bu1.io.origData := io.pipelineDecodeResult.regReadData.data2
  bu1.io.datas := stageDatas
  val regReadData2 = bu1.io.bypassData
  hazardStall(1) := bu1.io.stall

  val regData = Wire(new GPR.RegisterReadData)
  regData.data1 := regReadData1
  regData.data2 := regReadData2

  val mduBusy = Wire(Bool())
  val stallFromExecution = (hazardStall(0) && io.pipelineDecodeResult.controlSignal.regData1Stage <= reg_stage_exec) ||
    (hazardStall(1) && io.pipelineDecodeResult.controlSignal.regData1Stage <= reg_stage_exec) ||
    mduBusy
  io.stallFromExecution := stallFromExecution

  val stallOnExecution = stallFromExecution
  io.stallOnExecution := stallOnExecution

  val stall = stallOnExecution || io.pipelineDecodeResult.bubbled

  val op1 = getOperand(io.pipelineDecodeResult.controlSignal.aluOpFrom1, regData, io.pipelineDecodeResult.inst)
  val op2 = getOperand(io.pipelineDecodeResult.controlSignal.aluOpFrom2, regData, io.pipelineDecodeResult.inst)

  val alu = Module(new ALU)
  alu.io.op1 := op1
  alu.io.op2 := op2
  alu.io.op := io.pipelineDecodeResult.controlSignal.aluOp

  val mdu = Module(new MDU)
  mdu.io.op1 := op1
  mdu.io.op2 := op2
  mdu.io.op := io.pipelineDecodeResult.controlSignal.mduOp
  mdu.io.start := io.pipelineDecodeResult.controlSignal.startMdu && !stall
  mduBusy := mdu.io.busy

  val passBubble = stallFromExecution || io.pipelineDecodeResult.bubbled

  when (!stall) {
    pipelineExecutionResult.pc := io.pipelineDecodeResult.pc
    pipelineExecutionResult.pcChanged := io.pipelineDecodeResult.pcChanged
    pipelineExecutionResult.inst := io.pipelineDecodeResult.inst
    pipelineExecutionResult.controlSignal := io.pipelineDecodeResult.controlSignal
    pipelineExecutionResult.regReadId := io.pipelineDecodeResult.regReadId
    pipelineExecutionResult.regReadData := regData
    pipelineExecutionResult.aluResult := alu.io.result
    pipelineExecutionResult.dmAddr := alu.io.result
    pipelineExecutionResult.wRegId := io.pipelineDecodeResult.wRegId

    when (io.pipelineDecodeResult.controlSignal.wRegEnabled) {
      when (io.pipelineDecodeResult.wRegDataReady) {
        pipelineExecutionResult.wRegDataReady := 1.B
        pipelineExecutionResult.wRegData := io.pipelineDecodeResult.wRegData
      }.elsewhen(io.pipelineDecodeResult.controlSignal.wRegDataFrom === reg_write_from_alu) {
        pipelineExecutionResult.wRegDataReady := 1.B
        pipelineExecutionResult.wRegData := alu.io.result
      }.elsewhen(io.pipelineDecodeResult.controlSignal.wRegDataFrom === reg_write_from_mdu) {
        pipelineExecutionResult.wRegDataReady := 1.B
        pipelineExecutionResult.wRegData := mdu.io.data
      }.otherwise {
        pipelineExecutionResult.wRegDataReady := 0.B
        pipelineExecutionResult.wRegData := 0x5AAA555A.U(32.W)
      }
    }.otherwise {
      pipelineExecutionResult.wRegDataReady := 1.B
      pipelineExecutionResult.wRegData := 0x0D0D000E.U(32.W)
    }
  }

  pipelineExecutionResult.bubbled := passBubble

  when (pipelineExecutionResult.bubbled) {
    dataFromExec.regId := GPR.ZERO
    dataFromExec.dataReady := 1.B
    dataFromExec.data := 0.U
  }.otherwise {
    dataFromExec.regId := pipelineExecutionResult.wRegId
    dataFromExec.dataReady := pipelineExecutionResult.wRegDataReady
    dataFromExec.data := pipelineExecutionResult.wRegData
  }

}

object cExecution extends App {
  //  (new chisel3.stage.ChiselStage).emitVerilog(new GCD,args.union(Array("-td","v")).distinct)
  (new chisel3.stage.ChiselStage).emitVerilog(new cExecution, Array("-td", "v"))
}
