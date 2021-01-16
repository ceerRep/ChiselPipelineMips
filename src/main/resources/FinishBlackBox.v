module FinishBlackBox (input finish);
    always @* if (finish) $finish;
endmodule