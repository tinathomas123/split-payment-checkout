# Creates the three Lambdas, the Step Functions state machine and the IAM roles they need.
# Build the Lambdas first (npm run build in ../lambdas), so ../lambdas/dist exists.

locals {
  lambdas = {
    charge_card         = "chargeCard"
    refund_card         = "refundCard"
    update_order_status = "updateOrderStatus"
  }
}

# ---------------------------------------------------------------------------
# Lambda code: one zip of the compiled JavaScript; each function uses a different handler.
# ---------------------------------------------------------------------------
data "archive_file" "lambdas" {
  type        = "zip"
  source_dir  = "${path.module}/../lambdas/dist"
  output_path = "${path.module}/build/lambdas.zip"
  excludes    = [for f in fileset("${path.module}/../lambdas/dist", "__tests__/**") : f]
}

# ---------------------------------------------------------------------------
# IAM role for the Lambdas: allowed to run and write logs, nothing else.
# ---------------------------------------------------------------------------
data "aws_iam_policy_document" "lambda_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "lambda" {
  name               = "${var.project}-lambda-role"
  assume_role_policy = data.aws_iam_policy_document.lambda_assume_role.json
}

resource "aws_iam_role_policy_attachment" "lambda_logs" {
  role       = aws_iam_role.lambda.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

# ---------------------------------------------------------------------------
# Log groups created by Terraform (not automatically by Lambda), so terraform destroy removes them.
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_log_group" "lambda" {
  for_each          = local.lambdas
  name              = "/aws/lambda/${var.project}-${each.value}"
  retention_in_days = var.log_retention_days
}

resource "aws_lambda_function" "this" {
  for_each = local.lambdas

  function_name    = "${var.project}-${each.value}"
  role             = aws_iam_role.lambda.arn
  runtime          = "nodejs22.x"
  handler          = "${each.value}.handler" # e.g. chargeCard.js, exported "handler"
  filename         = data.archive_file.lambdas.output_path
  source_code_hash = data.archive_file.lambdas.output_base64sha256
  timeout          = 10
  memory_size      = 128

  depends_on = [aws_cloudwatch_log_group.lambda, aws_iam_role_policy_attachment.lambda_logs]
}

# ---------------------------------------------------------------------------
# IAM role for Step Functions: allowed to invoke only these three Lambdas.
# ---------------------------------------------------------------------------
data "aws_iam_policy_document" "sfn_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["states.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "sfn" {
  name               = "${var.project}-sfn-role"
  assume_role_policy = data.aws_iam_policy_document.sfn_assume_role.json
}

# Each Lambda ARN plus ":*", which covers invoking a specific version or alias.
data "aws_iam_policy_document" "sfn_invoke_lambdas" {
  statement {
    actions = ["lambda:InvokeFunction"]
    resources = flatten([
      for fn in aws_lambda_function.this : [fn.arn, "${fn.arn}:*"]
    ])
  }
}

resource "aws_iam_role_policy" "sfn_invoke_lambdas" {
  name   = "invoke-saga-lambdas"
  role   = aws_iam_role.sfn.id
  policy = data.aws_iam_policy_document.sfn_invoke_lambdas.json
}

# ---------------------------------------------------------------------------
# The state machine. The workflow is in state-machine.asl.json; Terraform fills in the Lambda ARNs.
# ---------------------------------------------------------------------------
resource "aws_sfn_state_machine" "split_payment_saga" {
  name     = "${var.project}-split-payment-saga"
  role_arn = aws_iam_role.sfn.arn
  type     = "STANDARD" # keeps a full, visual execution history in the console

  definition = templatefile("${path.module}/state-machine.asl.json", {
    charge_card_arn         = aws_lambda_function.this["charge_card"].arn
    refund_card_arn         = aws_lambda_function.this["refund_card"].arn
    update_order_status_arn = aws_lambda_function.this["update_order_status"].arn
  })

  depends_on = [aws_iam_role_policy.sfn_invoke_lambdas]
}
