﻿using Microsoft.Extensions.Logging;

using WasdiLib.Models;
using WasdiLib.Repositories;

namespace WasdiLib.Services
{
    internal class ProductService : IProductService
    {
        private readonly ILogger<ProductService> _logger;
        private readonly IProductRepository _repository;

        public ProductService(ILogger<ProductService> logger, IProductRepository repository)
        {
            _logger = logger;
            _repository = repository;
        }

        public List<Product> GetProductsByWorkspaceId(string sBaseUrl, string sSessionId, string sWorkspaceId)
        {
            _logger.LogDebug("GetProcessWorkspacesByProcessId({0})", sWorkspaceId);

            return _repository.GetProductsByWorkspaceId(sBaseUrl, sSessionId, sWorkspaceId).GetAwaiter().GetResult();
        }

    }
}
