output "state_machine_arn" {
  description = "Use this with: aws stepfunctions start-execution --state-machine-arn <arn> --input file://..."
  value       = aws_sfn_state_machine.split_payment_saga.arn
}

output "lambda_function_names" {
  value = [for fn in aws_lambda_function.this : fn.function_name]
}
