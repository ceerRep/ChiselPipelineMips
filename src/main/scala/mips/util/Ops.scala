package mips.util

import chisel3.util._

object Ops {
  val add :: sub :: shift_r_a :: shift_r_l :: shift_l :: and :: or :: xor :: nor :: sless :: uless :: Nil = Enum(11)
  val m_read_hi :: m_read_lo :: m_write_hi :: m_write_lo :: m_sign_mul :: m_unsign_mul :: m_sign_div :: m_unsign_div :: Nil = Enum(8)
}
