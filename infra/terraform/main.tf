terraform {
  required_version = ">= 1.4.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket = "your-tf-state-bucket"
    key    = "corebank-payment/terraform.tfstate"
    region = "ap-southeast-1"
  }
}

provider "aws" {
  region = var.aws_region
}

module "network" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "5.0.0"

  name = "corebank-lab-vpc"
  cidr = var.vpc_cidr

  azs             = var.availability_zones
  private_subnets = var.private_subnets
  public_subnets  = var.public_subnets

  enable_dns_hostnames = true
  enable_dns_support   = true

  enable_nat_gateway = true
  single_nat_gateway = true

  tags = local.common_tags
}

module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "20.8.3"

  cluster_name    = "corebank-lab-eks"
  cluster_version = "1.29"

  subnet_ids = module.network.private_subnets

  vpc_id = module.network.vpc_id

  eks_managed_node_groups = {
    corebank = {
      desired_size = 3
      min_size     = 2
      max_size     = 5

      instance_types = ["m6i.large"]
      ami_type       = "AL2_x86_64"

      tags = merge(local.common_tags, {
        "kubernetes.io/cluster/corebank-lab-eks" = "owned"
      })
    }
  }

  tags = local.common_tags
}

resource "aws_db_subnet_group" "corebank" {
  name       = "corebank-lab-db"
  subnet_ids = module.network.private_subnets

  tags = local.common_tags
}

resource "aws_db_parameter_group" "oracle" {
  name        = "corebank-lab-oracle-params"
  family      = "oracle-ee-19"
  description = "Parameter group for CoreBank lab"

  tags = local.common_tags
}

resource "aws_db_instance" "oracle" {
  identifier = "corebank-lab-oracle"

  engine               = "oracle-ee"
  engine_version       = "19.0.0.0.ru-2024-04.rur-2024-04.r1"
  instance_class       = "db.m6i.xlarge"
  allocated_storage    = 100
  max_allocated_storage = 500

  db_name  = "COREBANK"
  username = var.db_master_username
  password = var.db_master_password

  db_subnet_group_name    = aws_db_subnet_group.corebank.name
  vpc_security_group_ids  = [aws_security_group.db.id]
  parameter_group_name    = aws_db_parameter_group.oracle.name
  multi_az                = true
  storage_encrypted       = true
  publicly_accessible     = false
  backup_retention_period = 7

  deletion_protection = true
  skip_final_snapshot = false

  tags = local.common_tags
}

resource "aws_security_group" "db" {
  name        = "corebank-lab-db-sg"
  description = "Allow EKS nodes to access Oracle"
  vpc_id      = module.network.vpc_id

  ingress {
    description = "Oracle from EKS"
    from_port   = 1521
    to_port     = 1521
    protocol    = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.common_tags
}

locals {
  common_tags = {
    Project     = "CoreBankPaymentLab"
    Environment = var.environment
    Owner       = var.owner
  }
}
