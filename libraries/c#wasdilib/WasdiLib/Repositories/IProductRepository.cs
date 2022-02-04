﻿using WasdiLib.Models;

namespace WasdiLib.Repositories
{
    internal interface IProductRepository
    {

        Task<List<Product>> GetProductsByWorkspaceId(string sBaseUrl, string sSessionId, string sWorkspaceId);

        Task<PrimitiveResult> DeleteProduct(string sWorkspaceBaseUrl, string sSessionId, string sWorkspaceId, string sProduct);

    }
}
