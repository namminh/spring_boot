output "eks_cluster_name" {
  description = "Name of the EKS cluster"
  value       = module.eks.cluster_name
}

output "eks_cluster_endpoint" {
  description = "EKS cluster endpoint"
  value       = module.eks.cluster_endpoint
}

output "eks_cluster_ca" {
  description = "Cluster certificate authority data"
  value       = module.eks.cluster_certificate_authority_data
  sensitive   = true
}

output "eks_node_group_role_arn" {
  description = "IAM role used by the node group"
  value       = module.eks.eks_managed_node_groups["corebank"].iam_role_arn
}

output "rds_endpoint" {
  description = "Oracle RDS endpoint"
  value       = aws_db_instance.oracle.endpoint
}

output "rds_identifier" {
  description = "Oracle RDS identifier"
  value       = aws_db_instance.oracle.id
}
