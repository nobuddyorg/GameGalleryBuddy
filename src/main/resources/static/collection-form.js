function setIntParam(params, name, value) {
    if (value !== '') params.set(name, value);
}

function bindCollectionForm(formId) {
    var form = document.getElementById(formId);
    form.addEventListener('submit', function (evt) {
        evt.preventDefault();
        var f = evt.target;
        var params = new URLSearchParams();
        params.set('username', f.username.value.trim());
        setIntParam(params, 'size', f.size.value);
        params.set('showName', f.showName.checked);
        params.set('showUrl', f.showUrl.checked);
        params.set('shuffle', f.shuffle.checked);
        setIntParam(params, 'overflow', f.overflow.value);
        setIntParam(params, 'repeat', f.repeat.value);
        params.set('includePrevOwned', f.includePrevOwned.checked);
        window.location.href = '/collection?' + params.toString();
    });
}
