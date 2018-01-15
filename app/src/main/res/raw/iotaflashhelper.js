var Helper = {
    applyTransfers: function(flash, bundles) {
        iotaFlash.transfer.applyTransfers(
            flash.root,
            flash.deposits,
            flash.outputs,
            flash.remainderAddress,
            flash.transfers,
            bundles
        )
        return flash
    }
}
